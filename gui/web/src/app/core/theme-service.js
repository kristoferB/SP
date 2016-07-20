(function () {
    'use strict';

    angular
        .module('app.core')
        .factory('themeService', themeService);

    themeService.$inject = ['$localStorage', '$http', 'dashboardService'];
    /* @ngInject */
    function themeService($localStorage, $http, dashboardService) {

        var service = {
            storage: $localStorage.$default({
                gridsterConstants: {
                    margin: 10
                },
                // by default, less is unchanged
                lessColorConstants: {},
                lessLayoutConstants: {}
            }),

            showHeaders: true,
            showNavbar: true,

            setColorTheme: setColorTheme
        };

        activate();

        return service;

        function activate() {
            update();
        }

        function configureGridster(){
            dashboardService.setPanelMargins(service.storage.gridsterConstants.margin);
        }

        function compileLess(){
            //merge config variables into the .less file
            less.modifyVars(
                Object.assign(
                    service.storage.lessColorConstants,
                    service.storage.lessLayoutConstants
                )
            );
        }

        function setColorTheme(theme){
            assignFromServer("/style_presets/colors/"+theme, "lessColorConstants");
        }

        function setLayoutTheme(theme){
            assignFromServer("/style_presets/layouts/"+theme, "lessLayoutConstants");
        }

        function update(){
            compileLess();
            configureGridster();
        }

        function assignFromServer(url, variable){
            $http.get(url, "json").
                then(function successCallback(response) {
                    service.storage[variable] = response.data;
                    update();
                }, function errorCallback(response) {
                    console.log(response);
                }
            );
        }
    }
})();

