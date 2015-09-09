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
            $rootScope.$on('modelUpdate', function(event, model) {
                logger.info("modelversion should update")
                getDiffs(model);
            });
        }

	function getDiffs(model) {
      vm.diffs = model.history
	}

	function revert(versionNo) {
	    console.log('reverting: ' + versionNo);
	    restService.revertModel(modelService.activeModel.id, versionNo);
	    $state.go('dashboard');
	};
    }
})();
