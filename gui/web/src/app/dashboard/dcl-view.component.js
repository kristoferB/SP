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
var core_1 = require('@angular/core');
var DclViewComponent = (function () {
    function DclViewComponent(resolver) {
        this.resolver = resolver;
        this.isViewInitialized = false;
    }
    DclViewComponent.prototype.updateComponent = function () {
        var _this = this;
        if (!this.isViewInitialized) {
            return;
        }
        if (this.cmpRef) {
            this.cmpRef.destroy();
        }
        //    this.dcl.loadNextToLocation(this.type, this.target).then((cmpRef) => {
        this.resolver.resolveComponent(this.type).then(function (factory) {
            _this.cmpRef = _this.target.createComponent(factory);
        });
    };
    DclViewComponent.prototype.ngOnChanges = function () {
        this.updateComponent();
    };
    DclViewComponent.prototype.ngAfterViewInit = function () {
        this.isViewInitialized = true;
        this.updateComponent();
    };
    DclViewComponent.prototype.ngOnDestroy = function () {
        if (this.cmpRef) {
            this.cmpRef.destroy();
        }
    };
    __decorate([
        core_1.ViewChild('target', { read: core_1.ViewContainerRef }), 
        __metadata('design:type', Object)
    ], DclViewComponent.prototype, "target", void 0);
    __decorate([
        core_1.Input(), 
        __metadata('design:type', Object)
    ], DclViewComponent.prototype, "type", void 0);
    DclViewComponent = __decorate([
        core_1.Component({
            selector: 'dcl-view',
            template: '<div #target></div>'
        }), 
        __metadata('design:paramtypes', [core_1.ComponentResolver])
    ], DclViewComponent);
    return DclViewComponent;
}());
exports.DclViewComponent = DclViewComponent;
//# sourceMappingURL=dcl-view.component.js.map