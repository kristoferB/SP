'use strict';

describe('Controller: InteractivesopCtrl', function () {

  // load the controller's module
  beforeEach(module('spGuiApp'));

  var InteractivesopCtrl,
    scope;

  // Initialize the controller and a mock scope
  beforeEach(inject(function ($controller, $rootScope) {
    scope = $rootScope.$new();
    InteractivesopCtrl = $controller('InteractivesopCtrl', {
      $scope: scope
    });
  }));

  
});
