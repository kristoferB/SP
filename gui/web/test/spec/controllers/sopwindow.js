'use strict';

describe('Controller: SopwindowCtrl', function () {

  // load the controller's module
  beforeEach(module('spGuiApp'));

  var SopwindowCtrl,
    scope;

  // Initialize the controller and a mock scope
  beforeEach(inject(function ($controller, $rootScope) {
    scope = $rootScope.$new();
    SopwindowCtrl = $controller('SopwindowCtrl', {
      $scope: scope
    });
  }));

  
});
