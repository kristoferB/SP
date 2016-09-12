import { Component } from '@angular/core';

import { DROPDOWN_DIRECTIVES } from 'ng2-bootstrap';

@Component({
  selector: 'item-editor',
  templateUrl: 'app/ng2-item-editor/item-editor.component.html',
  directives: [DROPDOWN_DIRECTIVES]
})

export class ItemEditorComponent {

    // allting nonsens-satt for now
    numberOfErrors: number = 0;
    mode: string = 'IAmNull';
    modes: string[] = ['Nuuuull'];

    setMode(mode: string): void {
        console.log('called setMode');
    }

    atLeastOneItemChanged: boolean = false;
    save: () => void;
    expandAll: () => void;
    collapseAll: () => void;
    format: () => void;
    compact: () => void;
    _onUndo: () => void;
    canUndo: () => void;
    _onRedo: () => void;
    inSync: boolean = true;
    unSync: () => void;
    showDetail: boolean = false;

    editor: any;
    transformService: any;
    transform: () => void;
    editorLoaded: any;

    data: any;

    options: any;

    change: () => void;

   setActiveColor(number): void {
        console.log('called setActiveColor');
    }

}
