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
var upg_adapter_1 = require('../upg-helpers/upg-adapter');
var UpgNgInclude = upg_adapter_1.upgAdapter.upgradeNg1Component('upgNgInclude');
var ItemEditorComponent = (function () {
    function ItemEditorComponent() {
        this.path = "app/lazy-widgets/item-editor/item-editor.html";
    }
    ItemEditorComponent = __decorate([
        core_1.Component({
            template: '<upg-ng-include [src]="path"></upg-ng-include>',
            directives: [UpgNgInclude]
        }), 
        __metadata('design:paramtypes', [])
    ], ItemEditorComponent);
    return ItemEditorComponent;
}());
exports.ItemEditorComponent = ItemEditorComponent;
var ItemExplorerComponent = (function () {
    function ItemExplorerComponent() {
        this.path = "app/lazy-widgets/item-explorer/item-explorer.html";
    }
    ItemExplorerComponent = __decorate([
        core_1.Component({
            template: '<upg-ng-include [src]="path"></upg-ng-include>',
            directives: [UpgNgInclude]
        }), 
        __metadata('design:paramtypes', [])
    ], ItemExplorerComponent);
    return ItemExplorerComponent;
}());
exports.ItemExplorerComponent = ItemExplorerComponent;
var SOPMakerComponent = (function () {
    function SOPMakerComponent() {
        this.path = "app/lazy-widgets/sop-maker/sop-maker.html";
    }
    SOPMakerComponent = __decorate([
        core_1.Component({
            template: '<upg-ng-include [src]="path"></upg-ng-include>',
            directives: [UpgNgInclude]
        }), 
        __metadata('design:paramtypes', [])
    ], SOPMakerComponent);
    return SOPMakerComponent;
}());
exports.SOPMakerComponent = SOPMakerComponent;
var TrajectoriesComponent = (function () {
    function TrajectoriesComponent() {
        this.path = "app/lazy-widgets/trajectories/trajectories.html";
    }
    TrajectoriesComponent = __decorate([
        core_1.Component({
            template: '<upg-ng-include [src]="path"></upg-ng-include>',
            directives: [UpgNgInclude]
        }), 
        __metadata('design:paramtypes', [])
    ], TrajectoriesComponent);
    return TrajectoriesComponent;
}());
exports.TrajectoriesComponent = TrajectoriesComponent;
var OPCRunnerComponent = (function () {
    function OPCRunnerComponent() {
        this.path = "app/lazy-widgets/opc-runner/opc-runner.html";
    }
    OPCRunnerComponent = __decorate([
        core_1.Component({
            template: '<upg-ng-include [src]="path"></upg-ng-include>',
            directives: [UpgNgInclude]
        }), 
        __metadata('design:paramtypes', [])
    ], OPCRunnerComponent);
    return OPCRunnerComponent;
}());
exports.OPCRunnerComponent = OPCRunnerComponent;
var ProcessSimulateComponent = (function () {
    function ProcessSimulateComponent() {
        this.path = "app/lazy-widgets/process-simulate/process-simulate.html";
    }
    ProcessSimulateComponent = __decorate([
        core_1.Component({
            template: '<upg-ng-include [src]="path"></upg-ng-include>',
            directives: [UpgNgInclude]
        }), 
        __metadata('design:paramtypes', [])
    ], ProcessSimulateComponent);
    return ProcessSimulateComponent;
}());
exports.ProcessSimulateComponent = ProcessSimulateComponent;
var ConditionEditorComponent = (function () {
    function ConditionEditorComponent() {
        this.path = "app/lazy-widgets/condition-editor/condition-editor.html";
    }
    ConditionEditorComponent = __decorate([
        core_1.Component({
            template: '<upg-ng-include [src]="path"></upg-ng-include>',
            directives: [UpgNgInclude]
        }), 
        __metadata('design:paramtypes', [])
    ], ConditionEditorComponent);
    return ConditionEditorComponent;
}());
exports.ConditionEditorComponent = ConditionEditorComponent;
var OperationControlComponent = (function () {
    function OperationControlComponent() {
        this.path = "app/lazy-widgets/operation-control/operation-control.html";
    }
    OperationControlComponent = __decorate([
        core_1.Component({
            template: '<upg-ng-include [src]="path"></upg-ng-include>',
            directives: [UpgNgInclude]
        }), 
        __metadata('design:paramtypes', [])
    ], OperationControlComponent);
    return OperationControlComponent;
}());
exports.OperationControlComponent = OperationControlComponent;
var KubInputGUIComponent = (function () {
    function KubInputGUIComponent() {
        this.path = "app/lazy-widgets/kubInputGUI/kubInputGUI.html";
    }
    KubInputGUIComponent = __decorate([
        core_1.Component({
            template: '<upg-ng-include [src]="path"></upg-ng-include>',
            directives: [UpgNgInclude]
        }), 
        __metadata('design:paramtypes', [])
    ], KubInputGUIComponent);
    return KubInputGUIComponent;
}());
exports.KubInputGUIComponent = KubInputGUIComponent;
var OperatorInstGUIComponent = (function () {
    function OperatorInstGUIComponent() {
        this.path = "app/lazy-widgets/operatorInstGUI/operatorInstGUI.html";
    }
    OperatorInstGUIComponent = __decorate([
        core_1.Component({
            template: '<upg-ng-include [src]="path"></upg-ng-include>',
            directives: [UpgNgInclude]
        }), 
        __metadata('design:paramtypes', [])
    ], OperatorInstGUIComponent);
    return OperatorInstGUIComponent;
}());
exports.OperatorInstGUIComponent = OperatorInstGUIComponent;
var RobotCycleAnalysisComponent = (function () {
    function RobotCycleAnalysisComponent() {
        this.path = "app/lazy-widgets/robot-cycle-analysis/robot-cycle-analysis.html";
    }
    RobotCycleAnalysisComponent = __decorate([
        core_1.Component({
            template: '<upg-ng-include [src]="path"></upg-ng-include>',
            directives: [UpgNgInclude]
        }), 
        __metadata('design:paramtypes', [])
    ], RobotCycleAnalysisComponent);
    return RobotCycleAnalysisComponent;
}());
exports.RobotCycleAnalysisComponent = RobotCycleAnalysisComponent;
var ActiveOrderComponent = (function () {
    function ActiveOrderComponent() {
        this.path = "app/lazy-widgets/active-order/active-order.html";
    }
    ActiveOrderComponent = __decorate([
        core_1.Component({
            template: '<upg-ng-include [src]="path"></upg-ng-include>',
            directives: [UpgNgInclude]
        }), 
        __metadata('design:paramtypes', [])
    ], ActiveOrderComponent);
    return ActiveOrderComponent;
}());
exports.ActiveOrderComponent = ActiveOrderComponent;
var Tobbe2Component = (function () {
    function Tobbe2Component() {
        this.path = "app/lazy-widgets/Tobbe2/Tobbe2.html";
    }
    Tobbe2Component = __decorate([
        core_1.Component({
            template: '<upg-ng-include [src]="path"></upg-ng-include>',
            directives: [UpgNgInclude]
        }), 
        __metadata('design:paramtypes', [])
    ], Tobbe2Component);
    return Tobbe2Component;
}());
exports.Tobbe2Component = Tobbe2Component;
//# sourceMappingURL=upg-ng1-widget-containers.js.map