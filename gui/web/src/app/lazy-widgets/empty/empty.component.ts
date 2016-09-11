import { Component, OnInit } from '@angular/core';

@Component({
    selector: 'empty',
    templateUrl: 'app/lazy-widgets/empty/empty.component.html'
})
export class EmptyComponent {

    //aFunc: () => void;
    aFunc() {
        console.log('whoaow');
    }

    //constructor() {
    //    this.aFunc = () => {
    //        console.log('hej from aFunc');
    //    }
    //}

    //ngOnInit() {
    //    this.aFunc = () => {
    //        console.log('hej from aFunc in ngOninit');
    //    }
    //}

}
