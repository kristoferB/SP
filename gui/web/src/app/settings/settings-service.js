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

                gridlock: false,
                navbar_shown: true,
            }),
            availible_options: {
                saved_color_themes: ['default_white'],
                saved_layout_themes: ['standard']
            }, 
            
            showHeaders: true,
            theme_refreshed: theme_refreshed,
            getLayoutTheme: getLayoutTheme,
            getColorTheme: getColorTheme
        };

        activate();

        return service;

        function activate() {
            fetchSavedConfigs();
            theme_refreshed();
        }
        function theme_refreshed(){
            less.modifyVars(
                Object.assign(
                    getColorTheme(),
                    getLayoutTheme()
                )
            );
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
                }, function errorCallback(response) {
                    console.log("TODO: handle this error better");
                }
            );
        }
    }
})();

