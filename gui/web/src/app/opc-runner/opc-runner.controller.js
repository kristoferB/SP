/**
 * Created by Martin on 2015-11-19.
 */
(function () {
    'use strict';

    angular
      .module('app.opcRunner')
      .controller('opcRunnerController', opcRunnerController);

    opcRunnerController.$inject = ['opcRunnerService', '$scope', 'dashboardService','logger', 'modelService','itemService','restService','eventService','$interval'];
    /* @ngInject */
    function opcRunnerController(opcRunnerService, $scope, dashboardService, logger, modelService,itemService,restService,eventService,$interval) {
        var vm = this;

        vm.widget = $scope.$parent.$parent.$parent.vm.widget; //lol what

        vm.opcServ = opcRunnerService;
        vm.run_op = run_op;
        vm.get_init_state = get_init_state;
        vm.state = null;
        vm.enabled = [ ];
        vm.execute_op = execute_op;
        vm.selected = [ ];
        vm.reload_selection = reload_selection;
        vm.get_item_name = get_item_name;
        vm.get_item_state = get_item_state;
        vm.opsFilterQuery = "";
        vm.operations = [];
        vm.varsFilterQuery = "";
        vm.devsFilterQuery = "";
        vm.variables = "";
        vm.manualEnabled = [];
        vm.manualOpsFilterQuery = "";
        vm.get_autostart = get_autostart;
        vm.set_autostart = set_autostart;
        vm.force_finish = force_finish;

        // pose/state test
        vm.devices = [];
        var timer_stop;

        activate();

        var mutex = 0;

        function format_pose(p) {
            return _.map(p, format_double).join();
        }

        // really????
        function format_double(x) {
            return parseFloat(Math.round(x * 100) / 100).toFixed(2);
        }

        function pose_dist(p1,p2) {
            return Math.sqrt(_.foldl(_.zipWith(p1,p2,function(a,b){return (a-b)*(a-b);}),function(a,b){return a+b;}));
        }

        function get_pose_name(current_pose, poses) {
            var eps = 0.1; // arbitrary
            var pose = _.find(poses, function(p) {
                return pose_dist(current_pose, p.jointvalues) < eps && p.name !== 'HOME';
            });
            if(_.isUndefined(pose) || _.isUndefined(pose.name)) return "?";
            return pose.name;
        }

        function get_poses() {
            // test live pose get
            _.each(vm.devices, function (dev) {
                var idF = restService.getNewID();
                var answerF = idF.then(function(id){
                    var message = {
                        "setup": {
                            "command":"import single",
                            "txid":dev.item.attributes.txid
                        },
                        "core":{
                            "model":modelService.activeModel.id,
                            "responseToModel":false,
                            "includeIDAbles":[],
                            "onlyResponse":false
                        },
                        "reqID":id
                    };
                    return restService.postToServiceInstance(message, "ProcessSimulate")
                });
            });
        }

        function on_ps_event(event){
            // only care about OPC service, assume simulation service always finishes in time
            if(!(_.isUndefined(event.service)) && event.service != "ProcessSimulate") return;
            if(!(_.isUndefined(event.isa)) && event.isa != "Response") return;
            if(event.ids[0].attributes.current_pose.length === 0) return;
            var device = _.find(vm.devices, function (dev) { return dev.item.attributes.txid == event.ids[0].attributes.txid });
            if(_.isUndefined(device)) return;
            // fix 2*pi joint values for revolute joints...
            var current_pose = _.zipWith(event.ids[0].attributes.current_pose, device.item.attributes.joints, function (p,j) {
                while(j.type == 'Revolute' && p > 2*Math.PI) {
                    console.debug('fixing p-: ' + p);
                    p -= 2*Math.PI;
                }
                while(j.type == 'Revolute' && p < -2*Math.PI) {
                    console.debug('fixing p+: ' + p);
                    p += 2*Math.PI;
                }
                return p;
            });
            device.current_pose_name = get_pose_name(current_pose, event.ids[0].attributes.poses);
            device.current_pose = format_pose(current_pose);
        }

        function on_service_event(event){
            // only care about OPC service, assume simulation service always finishes in time
            if(!(_.isUndefined(event.service)) && event.service != "OpcRunner") return;
            if(!(_.isUndefined(event.isa)) && event.isa != "Response") return;
            console.log('operation finished ' + itemService.getItem(event.attributes.op).name);
            execute_op(vm.state, event.attributes.op);
        }

        function force_finish(opID) {
            if(vm.state[opID] == "e") {
                console.log('FORCE FINISHING OP: ' + opID);
                execute_op(vm.state, opID);
            }
        }

        function autostart() {
            // get autostart ops
            var auto_enabled = _.filter(vm.enabled, function(id) {
                var attr = itemService.getItem(id).attributes;
                return (attr.hasOwnProperty('autostart') && attr['autostart'])
            });
            // select random op and run it
            if(auto_enabled.length > 0) {
                var run_op = auto_enabled[Math.floor(Math.random()*auto_enabled.length)];
                console.log('starting op: ' + itemService.getItem(run_op).name);
                execute_op(vm.state,run_op);
            }
        }

        function activate() {
            $scope.$on('closeRequest', function() {
                dashboardService.closeWidget(vm.widget.id);
                // need this??
                vm.state = null;
                vm.enabled = [ ];
                vm.execute_op = execute_op;
                vm.selected = [ ];
                $interval.cancel(timer_stop);
                console.log('aborting simulation');

                // reset autoplay
                _.each(vm.selected, function (i) { itemService.getItem(i).autostart = false; });
            });

            eventService.addListener('ServiceError', on_service_event);
            eventService.addListener('Progress', on_service_event);
            eventService.addListener('Response', on_service_event);

            eventService.addListener('ServiceError', on_ps_event);
            eventService.addListener('Progress', on_ps_event);
            eventService.addListener('Response', on_ps_event);

            timer_stop = $interval(get_poses, 250);

            // listen to changes
            eventService.addListener('ModelDiff', on_model_diff);
            function on_model_diff(data) {
                console.log('RE-filter enabled ops...');
                if(vm.state) re_filter_enabled();
            }
        }

        function reload_selection() {
            vm.selected = _.map(itemService.selected, function(item){
                return item.id;
            });
        }

        function get_item_name(id) {
            return itemService.getItem(id).name
        }

        function get_item_state(item) {
            if(item.isa == 'Operation') {
                if(vm.state[item.id] == 'i') return 'idle';
                else if(vm.state[item.id] == 'e') return 'executing';
                else return 'error!';
            }
            if(item.isa == 'Thing') {
                var index = vm.state[item.id];
                return item.attributes.stateVariable.domain[index];
            }
        }

        function get_autostart(id) {
            var attr = itemService.getItem(id).attributes;
            return (attr.hasOwnProperty('autostart') && attr['autostart'])
        }

        function set_autostart(id, status) {
            itemService.getItem(id).attributes['autostart'] = status;
            itemService.saveItem(id);
        }

        function update_state(newstate,newenabled) {
            vm.state = newstate;
            vm.enabled = newenabled;

            // update state for gui
            vm.operations = [];
            vm.variables = [];
            _.each(vm.state, function(v,k) {  // note, value key reversed!
                var item = itemService.getItem(k);
                var add = { name: item.name, state: get_item_state(item), item: item };
                if(item.isa == 'Operation') {
                    vm.operations.push(add);
                } else if(item.isa == 'Thing') {
                    vm.variables.push(add);
                }
            });

            // in gui only show manually started ops
            vm.manualEnabled = _.map(_.filter(vm.enabled, function(id) {
                var attr = itemService.getItem(id).attributes;
                return !(attr.hasOwnProperty('autostart') && attr['autostart'])
            }), function(id) {
                return { id: id, name: itemService.getItem(id).name };
            });

            // let other functions update
            mutex=0;

            autostart();
        }

        function re_filter_enabled() {
            mutex=1;
            var idF = restService.getNewID();
            var answerF = idF.then(function(id){
                var message = {
                    "setup": {
                        "command":"re-filter enabled by state",
                        "state":vm.state,
                    },
                    "core":{
                        "model":modelService.activeModel.id,
                        "responseToModel":false,
                        "includeIDAbles":vm.selected,
                        "onlyResponse":true
                    },
                    "reqID":id
                };
                return restService.postToServiceInstance(message, "Simulation")
            });

            return answerF.then(function(serviceAnswer){
                update_state(serviceAnswer.attributes.newstate,serviceAnswer.attributes.enabled);
            })
        }

        function get_init_state() {
            // add all devices to pose-list
            var device_ids = _.filter(vm.selected, function(v) {
                return itemService.getItem(v).isa == 'Thing' &&
                    !_.isUndefined(itemService.getItem(v).attributes.current_pose) &&
                    itemService.getItem(v).attributes.current_pose.length > 0;
            });
            vm.devices = _.map(device_ids, function(dev) { return { item: itemService.getItem(dev), current_pose : [], current_pose_name: ""}});

            var idF = restService.getNewID();
            var answerF = idF.then(function(id){
                var message = {
                    "setup": {
                        "command":"get init state"
                    },
                    "core":{
                        "model":modelService.activeModel.id,
                        "responseToModel":false,
                        "includeIDAbles": vm.selected,
                        "onlyResponse":true
                    },
                    "reqID":id
                };
                return restService.postToServiceInstance(message, "Simulation")
            });

            return answerF.then(function(serviceAnswer){
                update_state(serviceAnswer.attributes.newstate,serviceAnswer.attributes.enabled);
            })
        }

        function execute_op(state,opID) {
            // onle let one op update at a time...
            if(mutex > 0) {
                setTimeout(function() { execute_op(vm.state,opID) }, 50);
                return;
            }
            mutex=1;

            // if starting op, start opc service
            if(state[opID] == "i") {
                run_op(opID);
            }

            var idF = restService.getNewID();
            var answerF = idF.then(function(id){
                var message = {
                    "setup": {
                        "command":"execute",
                        "state":state,
                        "operation":opID
                    },
                    "core":{
                        "model":modelService.activeModel.id,
                        "responseToModel":false,
                        "includeIDAbles":vm.selected,
                        "onlyResponse":true
                    },
                    "reqID":id
                };
                return restService.postToServiceInstance(message, "Simulation")
            });


            return answerF.then(function(serviceAnswer){
                update_state(serviceAnswer.attributes.newstate,serviceAnswer.attributes.enabled);
            })
        }

        function run_op(opID) {
            var idF = restService.getNewID();
            var answerF = idF.then(function(id){
                var message = {
                    "setup": {
                        "command":"start_op",
                        "ops":[opID]
                    },
                    "core":{
                        "model":modelService.activeModel.id,
                        "responseToModel":false,
                        "includeIDAbles":[],
                        "onlyResponse":false
                    },
                    "reqID":id
                };
                return restService.postToServiceInstance(message, "OpcRunner")
            });
        }
    }
})();
