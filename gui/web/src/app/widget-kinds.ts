import { WidgetKind } from './widget-kind';
import { Faces } from './erica-components/faces.component';
import { AwesomeNG2Component } from './lazy-widgets/ng2Inside/awesome-ng2-component.component';

export const widgetKinds: WidgetKind[] = [
    { 'component': Faces, 'title': 'ERICA Faces', 'sizex': 4, 'sizey': 4, 'id': null },
    { 'component': AwesomeNG2Component, 'title': 'ng2Inside', 'sizex': 4, 'sizey': 4, 'id': null }
]
