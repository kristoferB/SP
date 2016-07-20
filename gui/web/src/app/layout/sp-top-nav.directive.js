(function() {
    'use strict';

    angular
        .module('app.layout')
        .directive('htTopNav', htTopNav);

    /* @ngInject */
    function htTopNav () {
        var directive = {
            scope: {},
            bindToController: {
                'navline': '='
            },
            controller: TopNavController,
            controllerAs: 'vm',
            restrict: 'EA',
            templateUrl: 'app/layout/sp-top-nav.html'
        };

        return directive;
    }

    /* @ngInject */
    TopNavController.$inject = ['modelService', 'dashboardService', '$state', '$uibModal', 'settingsService', 'themeService'];

    function TopNavController(modelService, dashboardService, $state, $uibModal, settingsService, themeService) {
        var vm = this;
        vm.modelService = modelService;
        vm.models = modelService.models;
        vm.setActiveModel = modelService.setActiveModel;
        vm.activeModel = modelService.activeModel;

        vm.dashboardService = dashboardService;
        vm.settingsService = settingsService;
        vm.$state = $state;
        vm.createModel = createModel;
        vm.normalView = themeService.normalView;
        vm.compactView = themeService.compactView;
        vm.maximizedContentView = themeService.maximizedContentView;
        vm.layoutEditorView = themeService.layoutEditorView;
        vm.showNavbar = themeService.showNavbar;

        function createModel() {
            var modalInstance = $uibModal.open({
                templateUrl: '/app/models/createmodel.html',
                controller: 'CreateModelController',
                controllerAs: 'vm'
            });

            modalInstance.result.then(function(chosenName) {
                modelService.createModel(chosenName);
            });
        }
    }
})();
