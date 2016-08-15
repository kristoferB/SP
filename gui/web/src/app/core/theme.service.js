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
var Rx_1 = require("rxjs/Rx");
var ThemeService = (function () {
    function ThemeService(ng2DashboardService, //,
        http // eventually use the ng2 http here
        ) {
        var _this = this;
        this.ng2DashboardService = ng2DashboardService;
        this.coolSubscribe = function (callback) {
            _this.testSubject.subscribe(callback);
        };
        this.testSubject = new Rx_1.Subject();
        this.testColor = this.testSubject.asObservable();
        this.storage = {
            gridsterConstants: {
                margin: 10
            },
            // by default, less is unchanged
            lessColorConstants: new Rx_1.Observable(),
            lessLayoutConstants: new Rx_1.Observable()
        };
        this.showHeaders = true;
        this.showNavbar = true;
        this.showWidgetOptions = true;
        this.normalView = function () {
            _this.testSubject.next("blue");
            _this.currentView = "normalView";
            _this.storage.gridsterConstants.margin = 10;
            _this.showHeaders = true;
            _this.setLayoutTheme("normalView");
        };
        this.compactView = function () {
            _this.testSubject.next("green");
            _this.currentView = "compactView";
            _this.storage.gridsterConstants.margin = 3;
            _this.showHeaders = true;
            _this.setLayoutTheme("compactView");
        };
        this.maximizedContentView = function () {
            _this.currentView = "maximizedContentView";
            _this.storage.gridsterConstants.margin = 0;
            _this.showHeaders = false;
            _this.setLayoutTheme("maximizedContentView");
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
            _this.update();
        };
        this.editorModeEnabled = true;
        this.currentView = "test";
        this.setColorTheme = function (theme) {
            var _this = this;
            this.httpGet("/style_presets/colors/" + theme, function (res) {
                _this.storage.lessColorConstants = res;
                _this.update();
            });
        };
        this.setLayoutTheme = function (theme) {
            var _this = this;
            httpGet("/style_presets/layouts/" + theme, function (res) {
                _this.storage.lessLayoutConstants = res;
                _this.update();
            });
        };
        //  function resetGrid(){ //TODO rewrite this
        //  var navbarHeight = 0;
        //  if(this.showNavbar){
        //  navbarHeight = 50;
        //  }
        //  dashboardService.gridsterOptions.rowHeight = (window.innerHeight-navbarHeight) / 8;
        //  setTimeout(resetGrid, 0.3);
        //}
        this.configureGridster = function () {
            ng2DashboardService.setPanelMargins(_this.storage.gridsterConstants.margin);
        };
        this.compileLess = function () {
            //merge config variables into the .less file
            console.log('recompile less');
            // less.modifyVars(
            //     Object.assign(
            //         this.storage.lessColorConstants,
            //         this.storage.lessLayoutConstants,
            //         {showNavbar: this.showNavbar}
            //     )
            // );
        };
        this.update = function () {
            _this.compileLess();
            _this.configureGridster();
        };
        function httpGet(url, callback) {
            http.get(url, "json").
                then(function successCallback(response) {
                callback(response);
            }, function errorCallback(response) {
                console.log('http request errored');
                console.log(response);
            });
        }
        this.compileLess();
    }
    ThemeService = __decorate([
        core_1.Injectable(),
        //,
        __param(1, core_1.Inject('$http')), 
        __metadata('design:paramtypes', [ng2_dashboard_service_1.Ng2DashboardService, Object])
    ], ThemeService);
    return ThemeService;
}());
exports.ThemeService = ThemeService;
//# sourceMappingURL=theme.service.js.map