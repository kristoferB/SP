(function () {
    'use strict';

    angular
        .module('app.settings')
        .factory('settingsService', settingsService);

    settingsService.$inject = ['$localStorage', '$http'];
    /* @ngInject */
    function settingsService($localStorage, $http) {
        //For debugging: clears localstorage
        //$localStorage.$reset();
        
        var service = {
            storage: $localStorage.$default({
                saved_color_themes: {default_white: {}},
                saved_layout_themes: {
                    standard: {
                        gridster_panel_margin: 10
                    }
                },

                current_color_theme: 'default_white',
                current_layout_theme: 'standard',

                gridlock: false
                
            }),
            availible_options: {
                saved_color_themes: ['default_white'],
                saved_layout_themes: ['standard']
            }, 
            
            showHeaders: true,
            showNavbar: true,
            theme_refreshed: theme_refreshed,
            getLayoutTheme: getLayoutTheme,
            getColorTheme: getColorTheme,
            toggleNavbar: toggleNavbar
        };

        activate();

        return service;

        function activate() {
            fetchSavedConfigs();
            theme_refreshed();
        }
        function theme_refreshed(){
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
            theme_refreshed();
        }

        function toggleHeaders(){
            service.showHeaders = !service.showHeaders;
            theme_refreshed();
        }

        function getColorTheme(){
            return $localStorage.saved_color_themes[ $localStorage.current_color_theme];
        }

        function getLayoutTheme(){
            return $localStorage.saved_layout_themes[$localStorage.current_layout_theme];
        }

        function fetchSavedConfigs(){
            assignFromdatabase("/style_presets/layout_presets.json", "saved_layout_themes");
            assignFromdatabase("/style_presets/color_presets.json", "saved_color_themes");
        }

        function assignFromdatabase(url, variable){
            $http.get(url, "json").
                then(function successCallback(response) {
                    console.log(response);
                    $localStorage[variable] = response.data;
                    service.availible_options[variable] = [];
                    for(var key in response.data){
                        service.availible_options[variable].push(key);    
                    }
                    theme_refreshed();
                }, function errorCallback(response) {
                    console.log("TODO: handle this error better");
                }
            );
        }
    }
})();

