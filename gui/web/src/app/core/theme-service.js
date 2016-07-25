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
            showWidgetOptions: false,

            normalView: normalView,
            compactView: compactView,
            maximizedContentView: maximizedContentView,

            toggleNavbar: toggleNavbar,

            enableEditorMode: enableEditorMode,
            disableEditorMode: disableEditorMode,

            editorModeEnabled: false,

            currentView: "normalView",

            setColorTheme: setColorTheme
        };

        activate();

        return service;

        function activate() {
            update();
            resetGrid(); // TODO do this instead by listening to fullscreen events when this thing gets ng2-ed
        }

        function resetGrid(){ //TODO rewrite this
            var navbarHeight = 0;
            if(service.showNavbar){
                navbarHeight = 50;
            }
            dashboardService.gridsterOptions.rowHeight = (window.innerHeight-navbarHeight) / 8;
            setTimeout(resetGrid, 0.3);
        }

        function configureGridster(){
            dashboardService.setPanelMargins(service.storage.gridsterConstants.margin);
        }

        function compileLess(){
            //merge config variables into the .less file
            less.modifyVars(
                Object.assign(
                    service.storage.lessColorConstants,
                    service.storage.lessLayoutConstants,
                    {showNavbar: service.showNavbar}
                )
            );
        }

        function setColorTheme(theme){
            assignFromServer("/style_presets/colors/"+theme, "lessColorConstants");
        }

        function setLayoutTheme(theme){
            assignFromServer("/style_presets/layouts/"+theme, "lessLayoutConstants");
        }

        function toggleNavbar(){
            service.showNavbar = !service.showNavbar;
            console.log('waddup '+service.showNavbar);
            update();
        }

        function update(){
            compileLess();
            configureGridster();
        }

        function enableHeaders(){
            service.showHeaders = true;
        }

        function disableHeaders(){
            service.showHeaders = false;
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

        function normalView(){
            service.currentView = "normalView";
            service.storage.gridsterConstants.margin = 10;
            enableHeaders();
            setLayoutTheme("normalView");
        }

        function compactView(){
            service.currentView = "compactView";
            service.storage.gridsterConstants.margin = 3;
            enableHeaders();
            setLayoutTheme("compactView");
        }

        function maximizedContentView(){
            service.currentView = "maximizedContentView";
            service.storage.gridsterConstants.margin = 0;
            disableHeaders();
            setLayoutTheme("maximizedContentView");
        }

        function enableEditorMode(){
            service.editorModeEnabled = true;
            dashboardService.setPanelLock(true);
            service.showWidgetOptions = true;
        }

        function disableEditorMode(){
            service.editorModeEnabled = false;
            dashboardService.setPanelLock(false);
            service.showWidgetOptions = false;
        }
    }
})();

