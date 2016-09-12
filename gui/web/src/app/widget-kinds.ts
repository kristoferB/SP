import { WidgetKind } from './widget-kind';
import { Faces } from './erica-components/faces.component';
import { AwesomeNG2Component } from './lazy-widgets/ng2Inside/awesome-ng2-component.component';
import { Ng2ItemExplorerComponent } from './lazy-widgets/ng2-item-explorer/explorer.component';

import {
    ItemEditorComponent,
    ItemExplorerComponent,
    SOPMakerComponent,
    TrajectoriesComponent,
    OPCRunnerComponent,
    ProcessSimulateComponent,
    ConditionEditorComponent,
    OperationControlComponent,
    KubInputGUIComponent,
    OperatorInstGUIComponent,
    RobotCycleAnalysisComponent,
    ActiveOrderComponent,
    Tobbe2Component
} from './upg-helpers/upg-ng1-widget-containers';

export const widgetKinds: WidgetKind[] = [
    { 'component': Ng2ItemExplorerComponent, 'title': 'ng2-item-explorer', 'sizex': 20, 'sizey': 20, 'id': null, 'gridOptions': null },
    { 'component': Faces, 'title': 'ERICA Faces test', 'sizex': 4, 'sizey': 4, 'id': null, 'gridOptions': null },
    { 'component': AwesomeNG2Component, 'title': 'ng2Inside', 'sizex': 4, 'sizey': 4, 'id': null, 'gridOptions': null },
    { 'component': ItemExplorerComponent, 'title': 'item-explorer', 'sizex': 4, 'sizey': 4, 'id': null, 'gridOptions': null },
    { 'component': ItemEditorComponent, 'title': 'item-editor', 'sizex': 4, 'sizey': 4, 'id': null, 'gridOptions': null },
    { 'component': SOPMakerComponent, 'title': 'sop-maker', 'sizex': 4, 'sizey': 4, 'id': null, 'gridOptions': null },
    { 'component': OPCRunnerComponent, 'title': 'OPC-runner', 'sizex': 4, 'sizey': 4, 'id': null, 'gridOptions': null },
    { 'component': ProcessSimulateComponent, 'title': 'process-simulate', 'sizex': 4, 'sizey': 4, 'id': null, 'gridOptions': null },
    { 'component': ConditionEditorComponent, 'title': 'condition-editor', 'sizex': 4, 'sizey': 4, 'id': null, 'gridOptions': null },
    { 'component': OperationControlComponent, 'title': 'operation-control', 'sizex': 4, 'sizey': 4, 'id': null, 'gridOptions': null },
    { 'component': OperatorInstGUIComponent, 'title': 'operator-inst-gui', 'sizex': 4, 'sizey': 4, 'id': null, 'gridOptions': null },
    { 'component': RobotCycleAnalysisComponent, 'title': 'robot-cycle', 'sizex': 4, 'sizey': 4, 'id': null, 'gridOptions': null },
    { 'component': ActiveOrderComponent, 'title': 'active-order', 'sizex': 4, 'sizey': 4, 'id': null, 'gridOptions': null },
    { 'component': Tobbe2Component, 'title': 'tobbe-two', 'sizex': 4, 'sizey': 4, 'id': null, 'gridOptions': null }
]
