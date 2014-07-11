'use strict';

describe('Controller: ModellistmodalCtrl', function () {

  // load the controller's module
  beforeEach(module('spGuiApp'));

  var ModellistmodalCtrl,
    scope;

  // Initialize the controller and a mock scope
  beforeEach(inject(function ($controller, $rootScope) {
    scope = $rootScope.$new();
    ModellistmodalCtrl = $controller('ModellistmodalCtrl', {
      $scope: scope
    });
  }));


});
