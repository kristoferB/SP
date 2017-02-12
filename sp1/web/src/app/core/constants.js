/* global toastr:false, moment:false */
(function() {
    'use strict';

    angular
        .module('app.core')
        .constant('toastr', toastr)
        .constant('moment', moment)
        .constant('API', {
            events: '/api/events',
            models: '/api/models',
            modelImport: '/api/models/import',
            model: function(modelID) { return '/api/models/' + modelID; },
            modelExport: function(modelID) { return '/api/models/' + modelID + '/export'; },
            items: function(modelID) { return '/api/models/' + modelID + '/items'; },
            item: function(modelID, itemID) { return '/api/models/' + modelID + '/items/' + itemID; },
            modelDiff: function(modelID, version) { return '/api/models/' + modelID + '/history/diff?version='+version },
            revertModel: function(modelID, version) { return '/api/models/' + modelID + '/history/revert?version=' + version },
            runtimeKinds: '/api/runtimes/kinds',
            runtimeHandler: '/api/runtimes',
            runtimeInstance: function(runtimeID) { return '/api/runtimes/' + runtimeID; },
            stopRuntimeInstance: function(runtimeID) { return '/api/runtimes/' + runtimeID + '/stop'; },
            serviceHandler: '/api/services',
            serviceInstance: function(serviceInstanceID) { return '/api/services/' + serviceInstanceID; },
            newID: '/api/services/newid'
        });
})();
