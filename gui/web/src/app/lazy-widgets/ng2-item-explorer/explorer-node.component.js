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
var JsonNode_1 = require('./JsonNode');
var ItemExplorerNodeComponent = (function () {
    function ItemExplorerNodeComponent() {
    }
    __decorate([
        core_1.Input(), 
        __metadata('design:type', JsonNode_1.JsonNode)
    ], ItemExplorerNodeComponent.prototype, "node", void 0);
    ItemExplorerNodeComponent = __decorate([
        core_1.Component({
            selector: 'explorer-node',
            templateUrl: 'app/lazy-widgets/ng2-item-explorer/explorer-node.component.html',
            directives: [ItemExplorerNodeComponent]
        }), 
        __metadata('design:paramtypes', [])
    ], ItemExplorerNodeComponent);
    return ItemExplorerNodeComponent;
}());
exports.ItemExplorerNodeComponent = ItemExplorerNodeComponent;
//# sourceMappingURL=explorer-node.component.js.map