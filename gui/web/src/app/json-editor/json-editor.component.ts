import { Component, Renderer, ElementRef, ViewChild, AfterViewInit, Input } from '@angular/core';

// TODO sort out correct way of importing, i.e. as in commented line
//import { JSONEditor } from 'jsoneditor';
declare var JSONEditor: any;

@Component({
    selector: 'json-editor',
    template: '<div #editorElement style="height:100%;"></div>'
})
export class JsonEditorComponent {

    @Input() name;
    @ViewChild('editorElement') editorElement;

    editor: any;

    ngAfterViewInit() {
        this.editor = new JSONEditor(this.editorElement.nativeElement);
        this.editor.setName(this.name);
    }

    getJson() {
        return this.editor.get();
    }

    setJson(json: any) {
        this.editor.set(json)
    }

    setMode(mode: string) {
        this.editor.setMode(mode);
    }

    setName(name: string) {
        this.editor.setName(name);
    }

    fooFunction() {
        console.log("bananer");
    }
}
