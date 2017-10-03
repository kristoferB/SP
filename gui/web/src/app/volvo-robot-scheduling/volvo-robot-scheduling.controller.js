/**
 * Created by Martin on 2016-05-24.
 */
(function () {
    'use strict';

    angular
      .module('app.volvoRobotScheduling')
      .controller('volvoRobotSchedulingController', volvoRobotSchedulingController);

    volvoRobotSchedulingController.$inject = ['$scope', 'dashboardService','logger', 'modelService',
                                         'itemService', 'spServicesService', 'restService',
                                         'eventService'];
    /* @ngInject */
    function volvoRobotSchedulingController($scope, dashboardService, logger, modelService, itemService,
                                       spServicesService, restService, eventService) {
        var vm = this;

        vm.widget = $scope.$parent.$parent.$parent.vm.widget;
        vm.dashboard = $scope.$parent.$parent.$parent.vm.dashboard;

        vm.selectedSchedules = [];
        vm.removeSchedule = removeSchedule;
        vm.removeVar = removeVar;
        vm.verify = verify;
        vm.state = 'selecting';
        vm.GenerateSops = GenerateSops;
        vm.calculateUsingSops = calculateUsingSops;
        vm.loadCases = loadCases;
        vm.numStates = 0;
        vm.minTime = 0.0;
        vm.cpCompleted = false;
        vm.cpTime = 0.0;
        vm.sops = [];
        vm.openSOP = openSOP;
        vm.openGantt = openGantt;
        vm.bddName = '';
        vm.selectedVars = [];
        vm.selectedValues = {};
        vm.stateExists = 'uninitialized';
        vm.caseMap = {};
        vm.updateSelectedCases = updateSelectedCases;
        vm.checkSOP = checkSOP;
        vm.checkTime = checkTime;
        var SopID = '';
        var waitID = '';
        var caselist = {};
        var checkedTime = false;


document.getElementById("sopIDTextBox").onchange = function() {updateSopID()};


function checkSOP(){ checkedSOP = ! checkedSOP; }

function checkTime(){ checkedTime = ! checkedTime;}

function updateSelectedCases(k,v){
caselist[k] = v;
}


        function updateSelected(nowSelected, previouslySelected) {
            var n = _.difference(nowSelected, previouslySelected);
            if(vm.state == 'selecting') { // first we select schedules
                n = _.filter(n, function(x) { return !_.isUndefined(x.isa) && (x.isa == 'Operation'); }); // || x.isa == 'SOPSpec'
                vm.selectedSchedules = _.union(vm.selectedSchedules,n);
            } else if(vm.state == 'done') { // then we select variables
                n = _.filter(n, function(x) { return !_.isUndefined(x.isa) && x.isa == 'Thing'; });
                vm.selectedVars = _.union(vm.selectedVars,n);
            }
        }

        function actOnSelectionChanges() {
            $scope.$watchCollection(
                function() {
                    return itemService.selected;
                },
                updateSelected
            );
        }

        function removeSchedule(s) {
            vm.selectedSchedules = _.difference(vm.selectedSchedules,[s]);
        }

        function removeVar(v) {
            vm.selectedVars = _.difference(vm.selectedVars,[v]);
            vm.selectedValues = _.omitBy(vm.selectedValues,function(val,key) { return key == v.name; });
            verify();
        }

        function verify() {
            console.log('about to verify using bdd ' + vm.bddName);
            console.log(vm.selectedValues);
            var mess = {
                'core': {
                    'model': modelService.activeModel.id,
                    'responseToModel': false
                },
                'command': {
                    'bdd': vm.bddName,
                    'partialState': _.mapValues(vm.selectedValues, function(str) { return str * 1; }) // convert "1" to 1
                }
            };
            spServicesService.callService('BDDVerifier',{'data':mess},function(x){},function(x){}).then(function(repl){
                if(!_.isUndefined(repl.attributes.result)) {
                    if(repl.attributes.result) vm.stateExists = 'can';
                    else vm.stateExists = 'can not';
                } else vm.stateExists = '(an error has occured)';
            });
        }

        activate();



// This function listens to the message bus and depending on what ID's the messages have it takes action.
        function onEvent(ev){
        alert("new Event...");


            if(ev.reqID == waitID && vm.state == 'GenerateSops') {
                                SopID = ev.ids[0].id;
                                if(ev.ids.length == 0) {
                                                    alert("no sop!");
                                                }
                                else{
                                vm.state = 'GeneratedSops';
                                }
                              }
                else if(ev.reqID == waitID && vm.state == 'loadCases') {
                     vm.caseMap = ev.attributes['caseMap'];
                 }

            else if(ev.reqID == waitID && vm.state == 'calculateUsingSops') {
               console.log(ev.attributes);
                              vm.numStates = ev.attributes['numStates'];
                              vm.sops = ev.attributes['cpSops'];
                              vm.bddName = ev.attributes['bddName'];
                              if(vm.sops.length == 0) {
                                  vm.state = 'no sols';
                              } else {
                                  vm.minTime = vm.sops[0]._1;
                                  vm.cpCompleted = ev.attributes['cpCompleted'];
                                  vm.cpTime = ev.attributes['cpTime'];
                                  vm.state = 'done';
                              }
             }
        }
        function activate() {
            $scope.$on('closeRequest', function() {
                dashboardService.closeWidget(vm.widget.id);
                // maybe add some clean up here
            });
            eventService.addListener('Response', onEvent);
            actOnSelectionChanges();
        }



        function GenerateSops(){
            if(vm.selectedSchedules.length == 0) {
                             console.log('Must select a least one schedule');
                             return;
                         }
                         vm.state = 'GenerateSops';
                            var selected = _.map(vm.selectedSchedules, function(x) {return x.id;});
                          var mess = {
                          'command' : 'GenerateSops',
                          'SopID' : SopID,
                          'neglectedCases' : vm.caseMap,
                          'checkedTime' : checkedTime,
                              'core': {
                                  'model': modelService.activeModel.id,
                                  'responseToModel': true
                              },
                              'setup': {
                                  'selectedSchedules':selected
                              }
                          };
                          spServicesService.callService('VolvoRobotSchedule',{'data':mess},function(x){},function(x){}).then(function(repl){
                              waitID = repl.reqID;
                          });
        }

        function loadCases(){
            if(vm.selectedSchedules.length == 0) {
                             console.log('Must select a least one schedule');
                             return;
                         }
                         vm.state = 'loadCases';
                            var selected = _.map(vm.selectedSchedules, function(x) {return x.id;});
                          var mess = {
                          'command' : 'loadCases',
                          'SopID' : SopID,
                          'neglectedCases' : vm.caseMap,
                           'checkedTime' : checkedTime,
                              'core': {
                                  'model': modelService.activeModel.id,
                                  'responseToModel': true
                              },
                              'setup': {
                                  'selectedSchedules':selected
                              }
                          };
                          spServicesService.callService('VolvoRobotSchedule',{'data':mess},function(x){},function(x){}).then(function(repl){
                              waitID = repl.reqID;
                          });
        }

           function calculateUsingSops(){

            var caseMapdiff = {};

          for( var k in caselist){
               var a = vm.caseMap[k];
                var diff = a.filter(c => (c !== caselist[k]));
               caseMapdiff[k] = diff;
          }
                                             if(vm.selectedSchedules.length == 0) {
                                                                                 console.log('Must select a least one schedule');
                                                                                 return;
                                                                             }
                                                                             vm.state = 'calculateUsingSops';
                                                                                var selected = _.map(vm.selectedSchedules, function(x) {return x.id;});
                                                                              var mess = {
                                                                              'command' : 'calculateUsingSops',
                                                                              'SopID' : SopID,
                                                                              'neglectedCases' : caseMapdiff,
                                                                              'checkedTime' : checkedTime,
                                                                                  'core': {
                                                                                      'model': modelService.activeModel.id,
                                                                                      'responseToModel': true
                                                                                  },
                                                                                  'setup': {
                                                                                      'selectedSchedules':selected
                                                                                  }
                                                                              };
                                                                              spServicesService.callService('VolvoRobotSchedule',{'data':mess},function(x){},function(x){}).then(function(repl){
                                                                                  waitID = repl.reqID;
                                                                              });
                                                            }


        function openSOP(sopid) {
            var widgetKind = _.find(dashboardService.widgetKinds, {title: 'SOP Maker'});
            if (widgetKind === undefined) {
                logger.error('Open with SOP Maker failed, could not find widgetKind.');
            }
            dashboardService.addWidget(vm.dashboard, widgetKind, {sopSpecID: sopid});
        }

        function openGantt(gantt) {
            var widgetKind = _.find(dashboardService.widgetKinds, {title: 'Gantt Viewer'});
            if (widgetKind === undefined) {
                logger.error('Open with Gantt Viewer failed, could not find widgetKind.');
            }
            dashboardService.addWidget(vm.dashboard, widgetKind, {gantt: gantt});
        }
    }
})();
