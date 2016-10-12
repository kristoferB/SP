/**
 * Created by kristofer on 2016-10-10.
 */

  var sopView = function() {
  return {
    scope: {},
    bindToController: {
      sop: '<',
      items: '<'
    },
    controller: sopviewController,
    controllerAs: '$ctrl',
    template: `
        <div>HEJ MIN SOP</div>
    `,
    link: function(scope, element, attrs, ctrl) {
      var r = Raphael(element[0]);
      ctrl.r = r;
    }

  }};

  sopviewController.$inject = ['soopDraw', 'soopCalc'];
/* @ngInject */
  function sopviewController(soopDraw, soopCalc) {
    var ctrl = this;
    ctrl.r = null;



    ctrl.$onChanges = function(x){
      console.log("on changes in soop");
      console.log(ctrl.sop);
      console.log(x);
      console.log(ctrl.r);

      if (ctrl.sop != null && ctrl.r != null) {
        var tempS = {vm: {sopSpecCopy: ctrl.sop}};

        soopDraw.calcAndDrawSop(tempS, ctrl.r, true, true);
      }


    };

    ctrl.$onInit = function(){
      console.log("on init in soop");
      console.log(ctrl.sop);
      console.log(ctrl.r);
    };




  }



  angular.module('app.sopView').directive('soop', sopView);

