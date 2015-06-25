'use strict';

describe('Controller: SitecontrollerCtrl', function () {

  // load the controller's module
  beforeEach(module('spGuiApp'));

  var SitecontrollerCtrl,
    scope;

  // Initialize the controller and a mock scope
  beforeEach(inject(function ($controller, $rootScope) {
    scope = $rootScope.$new();
    SitecontrollerCtrl = $controller('SitecontrollerCtrl', {
      $scope: scope
    });
  }));

  
});
