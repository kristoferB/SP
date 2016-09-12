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
var explorer_service_1 = require('./explorer.service');
var explorer_node_component_1 = require('./explorer-node.component');
var Ng2ItemExplorerComponent = (function () {
    function Ng2ItemExplorerComponent(ng2ItemExplorerService) {
        var _this = this;
        this.subscriptions = {};
        this.modelNames = [];
        this.currentModel = {};
        this.service = ng2ItemExplorerService;
        this.selectModel = ng2ItemExplorerService.selectModel;
        this.refresh = function () {
            ng2ItemExplorerService.refresh();
        };
        this.subscriptions["modelNames"] = ng2ItemExplorerService.modelNames.subscribe(function (data) {
            _this.modelNames = data;
        });
        this.subscriptions["currentModel"] = ng2ItemExplorerService.currentModel.subscribe(function (data) {
            _this.currentModel = data;
            console.log(_this.currentModel);
        });
    }
    Ng2ItemExplorerComponent.prototype.ngOnInit = function () {
    };
    Ng2ItemExplorerComponent = __decorate([
        core_1.Component({
            selector: 'explorer',
            templateUrl: 'app/lazy-widgets/ng2-item-explorer/explorer.component.html',
            providers: [explorer_service_1.Ng2ItemExplorerService],
            directives: [explorer_node_component_1.ItemExplorerNodeComponent]
        }), 
        __metadata('design:paramtypes', [explorer_service_1.Ng2ItemExplorerService])
    ], Ng2ItemExplorerComponent);
    return Ng2ItemExplorerComponent;
}());
exports.Ng2ItemExplorerComponent = Ng2ItemExplorerComponent;
//# sourceMappingURL=explorer.component.js.map