import { Component, Renderer, ElementRef, ViewChild, AfterViewInit } from '@angular/core';

// TODO sort out correct way of importing, i.e. as in commented line
//import { JSONEditor } from 'jsoneditor';
declare var JSONEditor: any;

@Component({
    selector: 'json-editor',
    template: '{{ aString }}<div #editorElement style="height:100%;"></div>'
})
export class JsonEditorComponent {

    @ViewChild('editorElement') editorElement;

    editor: any;
    public aString: string = 'this is aString';

    ngAfterViewInit() {
        this.editor = new JSONEditor(this.editorElement.nativeElement);
        this.editor.set({"foo": "bar"});
    }

    getJson() {
        return this.editor.get();
    }

    setMode(mode: string) {
        this.editor.setMode(mode);
    }

    fooFunction() {
        console.log("bananer");
    }
}
