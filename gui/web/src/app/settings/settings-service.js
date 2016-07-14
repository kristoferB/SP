(function () {
    'use strict';

    angular
        .module('app.settings')
        .factory('settingsService', settingsService);

    settingsService.$inject = ['$sessionStorage', '$http', 'dashboardService'];
    /* @ngInject */
    function settingsService($sessionStorage, $http, dashboardService) { 
        // contains defaults for  all options that cannot be defined 
        // directly in the .less file.
        var defaults = {
            color: {
                default_white: {}
            },
            layout:{
                standard: {
                    gridster_panel_margin: 10
                }
            }                 
        };
        var service = {
            storage: $sessionStorage.$default({
                saved_color_themes: defaults.color,
                saved_layout_themes: defaults.layout,

                current_color_theme: 'default_white',
                current_layout_theme: 'standard'
            }),

            availible_options: {
                saved_color_themes: ['default_white'],
                saved_layout_themes: ['standard']
            }, 
            
            showHeaders: true,
            showNavbar: true,
            lockEnabled: true,

            compile_less: compile_less,
            getLayoutTheme: getLayoutTheme,
            getColorTheme: getColorTheme,
            toggleNavbar: toggleNavbar,
            togglePanelLock: togglePanelLock,
            update: update
        };

        activate();

        return service;

        function activate() {
            fetchSavedConfigs();
        }

        function configureGridster(){
            var margin = ($sessionStorage.saved_layout_themes[ $sessionStorage.current_layout_theme]).gridster_panel_margin;
            console.log(margin);
            dashboardService.setPanelMargins(margin);
        }

        function compile_less(){
            //merge config variables into the .less file
            less.modifyVars(
                Object.assign(
                    getColorTheme(),
                    getLayoutTheme(),
                    {showNavbar:service.showNavbar},
                    {showHeaders:service.showHeaders}  
                )
            );
        }

        function toggleNavbar(){
            service.showNavbar = !service.showNavbar;
            update();
        }

        function getColorTheme(){
            return $sessionStorage.saved_color_themes[ 
                $sessionStorage.current_color_theme];
        }

        function getLayoutTheme(){
            return $sessionStorage.saved_layout_themes[
                $sessionStorage.current_layout_theme];
        }

        function fetchSavedConfigs(){
            assignFromServer("/style_presets/layout_presets.json", "saved_layout_themes");
            assignFromServer("/style_presets/color_presets.json", "saved_color_themes");
            update();
        }

        function togglePanelLock(){
            service.lockEnabled = !service.lockEnabled;
            dashboardService.setPanelLock(service.lockEnabled);
        }

        function update(){
            compile_less();
            configureGridster();
        }

        function assignFromServer(url, variable){
            //fetches data from url and saves it in $sessionStorage.variable
            $http.get(url, "json").
                then(function successCallback(response) {
                    $sessionStorage[variable] = response.data;
                    service.availible_options[variable] = [];
                    for(var key in response.data){
                        service.availible_options[variable].push(key);    
                    }
                    update();
                }, function errorCallback(response) {
                    console.log(response);
                }
            );
        }
    }
})();

