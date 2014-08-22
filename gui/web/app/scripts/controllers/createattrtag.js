/**
 * Created by daniel on 2014-08-20.
 */

angular.module('spGuiApp')
  .controller('CreateAttrTagCtrl', function ($scope, $modalInstance, attrTagObj) {
    $scope.attrTagTypes = [
      {value: '', label: 'String'},
      {value: new Date(), label: 'Date'},
      {value: {}, label: 'Object'}
    ];

    $scope.forms = {
      tag: '',
      type: $scope.attrTagTypes[0]
    };

    $scope.close = function() {
      $modalInstance.dismiss('cancel');
    };

    $scope.save = function() {
      attrTagObj[$scope.forms.tag] = $scope.forms.type.value;
      $scope.close();
    };


  });
