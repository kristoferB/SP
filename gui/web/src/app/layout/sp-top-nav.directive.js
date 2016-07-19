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
    TopNavController.$inject = ['modelService', 'dashboardService', '$state', '$uibModal','settingsService'];

    function TopNavController(modelService, dashboardService, $state, $uibModal, settingsService) {
        var vm = this;
        vm.modelService = modelService;
        vm.models = modelService.models;
        vm.setActiveModel = modelService.setActiveModel;
        vm.activeModel = modelService.activeModel;

        vm.dashboardService = dashboardService;
        vm.settingsService = settingsService;
        vm.$state = $state;
        vm.createModel = createModel;
        vm.normalView = normalView;
        vm.compactView = compactView;
        vm.maximizedContentView = maximizedContentView;
        vm.layoutEditorView = layoutEditorView;
     

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

        function normalView(){
            vm.dashboardService.setPanelLock(false);
             dashboardService.setPanelMargins(10);
             settingsService.showHeaders = true;
        }
        
        function compactView(){
            dashboardService.setPanelLock(false);
            dashboardService.setPanelMargins(3);
            settingsService.showHeaders = true;
        }
        
        function maximizedContentView(){
            dashboardService.setPanelLock(false);
            dashboardService.setPanelMargins(0);
            settingsService.showHeaders = false;
        }

        function layoutEditorView(){
            dashboardService.setPanelLock(true);
            dashboardService.setPanelMargins(20);
            settingsService.showHeaders = true;
        }
    }
})();
