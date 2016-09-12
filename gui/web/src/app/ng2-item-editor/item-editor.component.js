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
var ng2_bootstrap_1 = require('ng2-bootstrap');
var ItemEditorComponent = (function () {
    function ItemEditorComponent() {
        // allting nonsens-satt for now
        this.numberOfErrors = 0;
        this.mode = 'IAmNull';
        this.modes = ['Nuuuull'];
        this.atLeastOneItemChanged = false;
        this.inSync = true;
        this.showDetail = false;
    }
    ItemEditorComponent.prototype.setMode = function (mode) {
        console.log('called setMode');
    };
    ItemEditorComponent.prototype.setActiveColor = function (number) {
        console.log('called setActiveColor');
    };
    ItemEditorComponent = __decorate([
        core_1.Component({
            selector: 'item-editor',
            templateUrl: 'app/ng2-item-editor/item-editor.component.html',
            directives: [ng2_bootstrap_1.DROPDOWN_DIRECTIVES]
        }), 
        __metadata('design:paramtypes', [])
    ], ItemEditorComponent);
    return ItemEditorComponent;
}());
exports.ItemEditorComponent = ItemEditorComponent;
//# sourceMappingURL=item-editor.component.js.map