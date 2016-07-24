import {Component, ComponentRef, ComponentFactory, Input, ViewContainerRef, ComponentResolver, ViewChild} from '@angular/core'


@Component({
  selector: 'dcl-view',
  template: `<div #target></div>`
})
export class DclViewComponent {
  @ViewChild('target', {read: ViewContainerRef}) target;
  @Input() type;
  cmpRef: ComponentRef<any>;
  private isViewInitialized: boolean = false;
  
  constructor(private resolver: ComponentResolver) {}

  updateComponent() {
    if(!this.isViewInitialized) {
      return;
    }
    if(this.cmpRef) {
      this.cmpRef.destroy();
    }
//    this.dcl.loadNextToLocation(this.type, this.target).then((cmpRef) => {
    this.resolver.resolveComponent(this.type).then((factory:ComponentFactory<any>) => {
      this.cmpRef = this.target.createComponent(factory)
    });
  }
  
  ngOnChanges() {
    this.updateComponent();
  }
  
  ngAfterViewInit() {
    this.isViewInitialized = true;
    this.updateComponent();  
  }
  
  ngOnDestroy() {
    if(this.cmpRef) {
      this.cmpRef.destroy();
    }    
  }
}
