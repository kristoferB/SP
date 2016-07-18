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
var upgrade_adapter_1 = require('../upgrade_adapter');
var ng2_ht_top_nav_component_1 = require('./ng2-ht-top-nav.component');
var Ng2ShellComponent = (function () {
    function Ng2ShellComponent(config, logger, $document, settingsService) {
        this.vm = {}; // TODO
        this.navline = { title: config.appTitle };
        this.config = config;
        this.logger = logger;
        this.$document = $document;
        this.settingsService = settingsService; // TODO
        this.vm.settingsService = settingsService; // TODO
        this.activate();
    }
    Ng2ShellComponent.prototype.activate = function () {
        this.giveFeedbackOnDropTargets();
        this.logger.log('Shell Controller: ' + this.config.appTitle +
            ' loaded!', null);
    };
    Ng2ShellComponent.prototype.giveFeedbackOnDropTargets = function () {
        this.$document.bind('dnd_move.vakata', onMove);
        function onMove(e, data) {
            var t = this.angular.element(data.event.target);
            if (!t.closest('.jstree').length) {
                if (t.closest('[drop-target]').length) {
                    data.helper.find('.jstree-icon')
                        .removeClass('jstree-er').addClass('jstree-ok');
                }
                else {
                    data.helper.find('.jstree-icon')
                        .removeClass('jstree-ok').addClass('jstree-er');
                }
            }
        }
    };
    Ng2ShellComponent = __decorate([
        core_1.Component({
            selector: 'ng2-shell',
            templateUrl: 'app/layout/shell.html',
            styleUrls: [],
            directives: [ng2_ht_top_nav_component_1.Ng2HtTopNavComponent,
                upgrade_adapter_1.upgradeAdapter.upgradeNg1Component('upgUiView')],
            providers: []
        }),
        __param(0, core_1.Inject('config')),
        __param(1, core_1.Inject('logger')),
        __param(2, core_1.Inject('$document')),
        __param(3, core_1.Inject('settingsService')), 
        __metadata('design:paramtypes', [Object, Object, Object, Object])
    ], Ng2ShellComponent);
    return Ng2ShellComponent;
}());
exports.Ng2ShellComponent = Ng2ShellComponent;
//# sourceMappingURL=ng2-shell.component.js.map