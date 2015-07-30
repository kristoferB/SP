/* jshint -W079 */
var mockData = (function() {
    return {
        // REST responses
        getMockStates: getMockStates,
        getMockModels: getMockModels,
        getMockItems: getMockItems,
        getMockRuntimeKinds: getMockRuntimeKinds,
        getMockRuntimeInstances: getMockRuntimeInstances,
        getMockRegisteredServices: getMockRegisteredServices,
        // SSE events
        getMockModelCreationEvent: getMockModelCreationEvent
    };

    // REST responses //

    function getMockStates() {
        return [
            {
                state: 'dashboard',
                config: {
                    url: '/',
                    templateUrl: 'app/dashboard/dashboard.html',
                    title: 'dashboard',
                    settings: {
                        nav: 1,
                        content: '<i class="fa fa-dashboard"></i> Dashboard'
                    }
                }
            }
        ];
    }

    function getMockModels() {
        return [
            {id:'5689ce13-2e2d-4451-8f8f-f05973f78dfb', name:'A first model', version:46,
                attributes:{time:'2015-07-07T18:22:07.125+0200'}},
            {id:'5ccdc932-438d-4c45-b947-6dd208e31e31', name:'A second model', version:72,
                attributes:{time:'2015-07-07T15:10:16.970+0200'}}
        ];
    }

    function getMockItems() {
        return [
            {isa: 'Thing', name: 'R2', attributes: {},
                id: 'f17b5f44-6e18-486c-9715-a8d9e24a1ea1'},
            {isa: 'Operation', name: 'R2ToHome', conditions: [], attributes: {},
                id: 'c6d1591c-b29a-4eb4-a42d-160a515af7ea'},
            {isa: 'Operation', name: 'R2ToTable', conditions: [], attributes: {},
                id: 'c6d1591c-b29a-4eb4-a42d-160a515af7eb'},
            {isa: 'SPSpec', name: 'Matrikon OPC', attributes: {},
                id: '1aba7054-9f3f-4a6b-88eb-ca2527366898'},
            {isa: 'SOPSpec', name: 'One at a time', sop: [], attributes: {},
                id: 'abd80a6a-f2f6-49fc-830f-5b2e27a5c2d1'},
            {isa: 'SOPSpec', name: 'First A, then B', sop: [], attributes: {},
                id: 'abd80a6a-f2f6-49fc-830f-5b2e27a5c2d2'},
            {isa: 'SOPSpec', name: 'Finish with A', sop: [], attributes: {},
                id: 'abd80a6a-f2f6-49fc-830f-5b2e27a5c2d3'}
        ];
    }

    function getMockRuntimeKinds() {
        return [
            {name: 'SimulationRuntime', attributes: {info: 'Simulate system behavior by executing operations'}},
            {name: 'PLCRuntime', attributes: {info: 'Show status of and control a PLC'}}
        ];
    }

    function getMockRuntimeInstances() {
        return [
            {id: 'f34ee181-06d3-442a-ae5d-4a2bc8e96d3c', kind: 'PLCRuntime',
                model: '5689ce13-2e2d-4451-8f8f-f05973f78dfb', name: 'GUI for PSL'},
            {id: '860beadb-c8cd-425e-98b8-9538716e16d8', kind: 'SimulationRuntime',
                model: '5ccdc932-438d-4c45-b947-6dd208e31e31', name: 'PSL Simulation'}
        ];
    }

    function getMockRegisteredServices() {
        return [
            'SynthesizeModel-prePostGuardsActionsInAttributes',
            'ProcessSimulate',
            'PropositionParser',
            'CreateInstanceModelFromTypeModel',
            'ImportJSONService',
            'ConditionsFromSpecsService',
            'SOPMaker',
            'SynthesizeModel-carrierAndResourceTrans',
            'CreateOpsFromManualModel',
            'Relations'
        ];
    }

    // SSE events //
    function getMockModelCreationEvent() {
        var dataObj = {
            target: 'ModelHandler',
            event: 'Creation',
            modelInfo: {
                id: 'a34ee181-06d3-442a-ae5d-422b38e9ed3d',
                name: 'A third model',
                version: 1,
                attributes: {
                    time: '2015-07-26T14:23:07.276+0200'
                }
            }
        };
        return createMessageEvent('ModelHandler', dataObj);
    }

    function createMessageEvent(eventName, dataObj) {
        var bubbles = false;
        var cancelable = false;
        var dataObjAsJson = angular.toJson(dataObj);
        var origin = 'http://localhost:3000';
        var lastEventId = '1';
        var source = window;
        var evt = document.createEvent('MessageEvent');  // MUST be 'MessageEvent'
        evt.initMessageEvent(eventName, bubbles, cancelable, dataObjAsJson, origin, lastEventId, source, null);
        //This deprecated way of creating a MessageEvent is required by PhantomJS 1.9.8

        return evt;
    }

})();
