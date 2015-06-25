'use strict';

describe('Controller: LoadfileCtrl', function () {

  // load the controller's module
  beforeEach(module('spGuiApp'));

  var LoadfileCtrl,
    scope;

  // Initialize the controller and a mock scope
  beforeEach(inject(function ($controller, $rootScope) {
    scope = $rootScope.$new();
    LoadfileCtrl = $controller('LoadfileCtrl', {
      $scope: scope
    });
  }));

  it('should attach a list of awesomeThings to the scope', function () {
    expect(scope.awesomeThings.length).toBe(3);
  });
});
