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
var core_1 = require('@angular/core');
var ng2_bootstrap_1 = require('ng2-bootstrap');
var upgrade_adapter_1 = require('../upgrade_adapter');
var Ng2HtTopNavComponent = (function () {
    function Ng2HtTopNavComponent(settingsService) {
        this.showNavbar = false;
        this.showEd = false;
        this.showNavbar = settingsService.showNavbar;
        this.togglePanelLock = settingsService.togglePanelLock;
        this.toggleNavbar = settingsService.toggleNavbar;
    }
    Ng2HtTopNavComponent.prototype.toggleEdVisible = function () {
        this.showEd = !this.showEd;
    };
    Ng2HtTopNavComponent = __decorate([
        core_1.Component({
            selector: 'ng2-ht-top-nav',
            templateUrl: 'app/layout/ht-top-nav.html',
            styleUrls: [],
            directives: [upgrade_adapter_1.upgradeAdapter.upgradeNg1Component('upgUserDropdown'),
                upgrade_adapter_1.upgradeAdapter.upgradeNg1Component('upgTopNavElements'),
                ng2_bootstrap_1.DROPDOWN_DIRECTIVES],
            providers: []
        }),
        __param(0, core_1.Inject('settingsService')), 
        __metadata('design:paramtypes', [Object])
    ], Ng2HtTopNavComponent);
    return Ng2HtTopNavComponent;
}());
exports.Ng2HtTopNavComponent = Ng2HtTopNavComponent;
//# sourceMappingURL=ng2-ht-top-nav.component.js.map