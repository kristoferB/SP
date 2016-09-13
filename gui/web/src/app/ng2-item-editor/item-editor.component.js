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
var _ = require('lodash');
var json_editor_component_1 = require('../json-editor/json-editor.component');
var event_bus_service_1 = require('../core/event-bus.service');
var ItemEditorComponent = (function () {
    function ItemEditorComponent(itemService, eventBusService) {
        var _this = this;
        // allting nonsens-satt for now
        this.numberOfErrors = 0;
        this.modes = ['tree', 'code'];
        this.editorName = 'Selected items';
        this.eventCallback = function (data) {
            var json = {};
            for (var _i = 0, data_1 = data; _i < data_1.length; _i++) {
                var j = data_1[_i];
                console.log(j);
                var item = _this.itemService.getItem(j);
                var keyName = item.name;
                var count = 0;
                while (!_.isUndefined(_.find(Object.keys(json), function (k) { return k == keyName; }))) {
                    keyName = item.name + ' (' + (++count) + ')';
                }
                json[keyName] = item;
            }
            _this.jec.setJson(json);
        };
        this.itemService = itemService;
        this.eventBusService = eventBusService;
        eventBusService.subscribeToTopic("minTopic", function () {
        }, this.eventCallback);
        setTimeout(function () {
            eventBusService.tweetToTopic("minTopic", _this.itemService.items.map(function (x) { return x.id; }));
        }, 2000);
        this.options = { mode: 'tree' };
        this.save = function () {
            var json = _this.jec.getJson();
            var keys = Object.keys(json);
            for (var _i = 0, keys_1 = keys; _i < keys_1.length; _i++) {
                var key = keys_1[_i];
                if (json.hasOwnProperty(key)) {
                    var editorItem = json[key];
                    var centralItem = _this.itemService.getItem(editorItem.id);
                    if (!_.isEqual(editorItem, centralItem)) {
                        _this.itemService.saveItem(editorItem);
                    }
                }
            }
        };
        this.setMode = function (mode) {
            _this.options.mode = mode;
            _this.jec.setMode(mode);
        };
    }
    ItemEditorComponent.prototype.ngOnDestroy = function () {
        this.eventBusService.unsubscribeToTopic("minTopic", this.eventCallback);
    };
    __decorate([
        core_1.Input(), 
        __metadata('design:type', Object)
    ], ItemEditorComponent.prototype, "widget", void 0);
    __decorate([
        core_1.ViewChild(json_editor_component_1.JsonEditorComponent), 
        __metadata('design:type', json_editor_component_1.JsonEditorComponent)
    ], ItemEditorComponent.prototype, "jec", void 0);
    ItemEditorComponent = __decorate([
        core_1.Component({
            selector: 'item-editor',
            templateUrl: 'app/ng2-item-editor/item-editor.component.html',
            directives: [ng2_bootstrap_1.DROPDOWN_DIRECTIVES, json_editor_component_1.JsonEditorComponent]
        }),
        __param(0, core_1.Inject('itemService')), 
        __metadata('design:paramtypes', [Object, event_bus_service_1.EventBusService])
    ], ItemEditorComponent);
    return ItemEditorComponent;
}());
exports.ItemEditorComponent = ItemEditorComponent;
//# sourceMappingURL=item-editor.component.js.map