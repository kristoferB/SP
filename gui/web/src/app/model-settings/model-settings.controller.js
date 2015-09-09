(function () {
    'use strict';

    angular
        .module('app.modelSettings')
        .controller('ModelSettingsController', ModelSettingsController);

    ModelSettingsController.$inject = ['$rootScope','logger','$state','API','restService','modelService'];
    /* @ngInject */
    function ModelSettingsController($rootScope,logger,$state,API,restService,modelService) {
        var vm = this;
        vm.title = $state.current.title;
        vm.revert = revert;
	vm.diffs = [];
	vm.displayedDiffs = [];
	vm.modelService = modelService;

        activate();

        function activate() {
            logger.info('ModelSettings Controller: Activated Model Settings View');

            if(modelService.activeModel !== null) {
		// already loaded
		getDiffs(modelService.activeModel);
            }
	    // will trigger on model load/change
            $rootScope.$on('modelChanged', function(event, model) {
                getDiffs(model);
            });
        }

	function getDiffs(model) {
	    vm.diffs.splice(0,vm.diffs.length);
            const max_diffs = 10; // cannot load more via rest service?
	    for(var i = model.version; (model.version - i) < max_diffs && i >= 1; i--) {
	    	var res = restService.getModelDiff(model.id,i).then(function(data) {
	    	    vm.diffs.push(data);
	    	});
	    }
	}

	function revert(versionNo) {
	    console.log('reverting: ' + versionNo);
	    restService.revertModel(modelService.activeModel.id, versionNo);
	    $state.go('dashboard');
	};
    }
})();
