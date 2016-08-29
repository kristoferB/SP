import { Component, Inject } from '@angular/core';

import { upgAdapter } from '../upg-helpers/upg-adapter';

const UpgNgInclude = upgAdapter.upgradeNg1Component('upgNgInclude');

@Component({
  template: '<upg-ng-include [src]="path"></upg-ng-include>',
  directives: [UpgNgInclude]
})
export class ItemEditorComponent {
    path: string = "app/lazy-widgets/item-editor/item-editor.html"
}

@Component({
  template: '<upg-ng-include [src]="path"></upg-ng-include>',
  directives: [UpgNgInclude]
})
export class ItemExplorerComponent {
    path: string = "app/lazy-widgets/item-explorer/item-explorer.html"
}

@Component({
  template: '<upg-ng-include [src]="path"></upg-ng-include>',
  directives: [UpgNgInclude]
})
export class SOPMakerComponent {
    path: string = "app/lazy-widgets/sop-maker/sop-maker.html"
}

@Component({
  template: '<upg-ng-include [src]="path"></upg-ng-include>',
  directives: [UpgNgInclude]
})
export class TrajectoriesComponent {
    path: string = "app/lazy-widgets/trajectories/trajectories.html"
}

@Component({
  template: '<upg-ng-include [src]="path"></upg-ng-include>',
  directives: [UpgNgInclude]
})
export class OPCRunnerComponent {
    path: string = "app/lazy-widgets/opc-runner/opc-runner.html"
}

@Component({
  template: '<upg-ng-include [src]="path"></upg-ng-include>',
  directives: [UpgNgInclude]
})
export class ProcessSimulateComponent {
    path: string = "app/lazy-widgets/process-simulate/process-simulate.html"
}

@Component({
  template: '<upg-ng-include [src]="path"></upg-ng-include>',
  directives: [UpgNgInclude]
})
export class ConditionEditorComponent {
    path: string = "app/lazy-widgets/condition-editor/condition-editor.html"
}

@Component({
  template: '<upg-ng-include [src]="path"></upg-ng-include>',
  directives: [UpgNgInclude]
})
export class OperationControlComponent {
    path: string = "app/lazy-widgets/operation-control/operation-control.html"
}

@Component({
  template: '<upg-ng-include [src]="path"></upg-ng-include>',
  directives: [UpgNgInclude]
})
export class KubInputGUIComponent {
    path: string = "app/lazy-widgets/kubInputGUI/kubInputGUI.html";
}

@Component({
  template: '<upg-ng-include [src]="path"></upg-ng-include>',
  directives: [UpgNgInclude]
})
export class OperatorInstGUIComponent {
    path: string = "app/lazy-widgets/operatorInstGUI/operatorInstGUI.html"
}

@Component({
  template: '<upg-ng-include [src]="path"></upg-ng-include>',
  directives: [UpgNgInclude]
})
export class RobotCycleAnalysisComponent {
    path: string = "app/lazy-widgets/robot-cycle-analysis/robot-cycle-analysis.html"
}

@Component({
  template: '<upg-ng-include [src]="path"></upg-ng-include>',
  directives: [UpgNgInclude]
})
export class ActiveOrderComponent {
    path: string = "app/lazy-widgets/active-order/active-order.html"
}

@Component({
  template: '<upg-ng-include [src]="path"></upg-ng-include>',
  directives: [UpgNgInclude]
})
export class Tobbe2Component {
    path: string = "app/lazy-widgets/Tobbe2/Tobbe2.html"
}
