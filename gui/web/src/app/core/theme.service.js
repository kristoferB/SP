"use strict";
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};
var __param = (this && this.__param) || function (paramIndex, decorator) {
    return function (target, key) { decorator(target, key, paramIndex); }
};
// //import { Ng2DashboardService } from '../dashboard/ng2-dashboard.service';
var core_1 = require("@angular/core");
var ng2_dashboard_service_1 = require("../dashboard/ng2-dashboard.service");
var platform_browser_1 = require("@angular/platform-browser");
var ThemeService = (function () {
    function ThemeService(ng2DashboardService, //,
        http, // eventually use the ng2 http here
        document) {
        // this does not need to exist; functional fo' life
        // this.storage = {
        //     gridsterConstants: {
        //         margin: 10
        //     },
        //     // by default, less is unchanged
        //     lessColorConstants: new Observable<Object>(),
        //     lessLayoutConstants: new Observable<Object>()
        // };
        var _this = this;
        this.ng2DashboardService = ng2DashboardService;
        this.document = document;
        this.showHeaders = true;
        this.showNavbar = true;
        this.showWidgetOptions = true;
        this.normalView = function () {
            _this.currentView = "normalView";
            ng2DashboardService.setPanelMargins(10);
            _this.showHeaders = true;
            _this.setLayoutTheme("default");
        };
        this.compactView = function () {
            _this.currentView = "compactView";
            ng2DashboardService.setPanelMargins(3);
            _this.showHeaders = true;
            _this.setLayoutTheme("compact");
        };
        this.maximizedContentView = function () {
            _this.currentView = "maximizedContentView";
            ng2DashboardService.setPanelMargins(0);
            _this.showHeaders = false;
            _this.setLayoutTheme("maximized_content");
        };
        this.enableEditorMode = function () {
            _this.editorModeEnabled = true;
            ng2DashboardService.setPanelLock(true);
            _this.showWidgetOptions = true;
        };
        this.disableEditorMode = function () {
            _this.editorModeEnabled = false;
            ng2DashboardService.setPanelLock(false);
            _this.showWidgetOptions = false;
        };
        this.toggleNavbar = function () {
            _this.showNavbar = !_this.showNavbar;
            //this.update();
        };
        this.editorModeEnabled = true;
        this.currentView = "test";
        this.setColorTheme = function (theme) {
            // this.httpGet(
            //     "/style_presets/colors/" + theme,
            //     (res: Object) => {
            //     }
            // )
            _this.document.getElementById('color_theme').setAttribute('href', '../.tmp/color/' + theme + '.css');
        };
        this.setLayoutTheme = function (theme) {
            // this.httpGet(
            //     "/style_presets/layouts/" + theme,
            //     (res: Object) => {
            //     }
            // );
            _this.document.getElementById('layout_theme').setAttribute('href', '../.tmp/layout/' + theme + '.css');
        };
        //  function resetGrid(){ //TODO rewrite this
        //  var navbarHeight = 0;
        //  if(this.showNavbar){
        //  navbarHeight = 50;
        //  }
        //  dashboardService.gridsterOptions.rowHeight = (window.innerHeight-navbarHeight) / 8;
        //  setTimeout(resetGrid, 0.3);
        //  }
        //
        // this.httpGet = (url: string, callback: (res: string) => any) => {
        //     http.get(url, "json").
        //     then(function successCallback(response) {
        //             callback(response)
        //         }, function errorCallback(response) {
        //             console.log('http request errored');
        //             console.log(response);
        //         }
        //     );
        // };
        this.configureGridster = function () {
            //ng2DashboardService.setPanelMargins(this.storage.gridsterConstants.margin);
        };
    }
    ThemeService = __decorate([
        core_1.Injectable(),
        //,
        __param(1, core_1.Inject('$http')),
        // eventually use the ng2 http here
        __param(2, core_1.Inject(platform_browser_1.DOCUMENT)), 
        __metadata('design:paramtypes', [ng2_dashboard_service_1.Ng2DashboardService, Object, Object])
    ], ThemeService);
    return ThemeService;
}());
exports.ThemeService = ThemeService;
//# sourceMappingURL=theme.service.js.map