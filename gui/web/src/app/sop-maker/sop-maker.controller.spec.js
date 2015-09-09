/* jshint -W117, -W030 */
describe('SOPMakerController', function() {
    var controller;

    beforeEach(function() {
        bard.appModule('app.sopMaker');
        bard.inject('$controller', '$log', '$rootScope', '$timeout');
        controller = $controller('SOPMakerController');
        $rootScope.$apply();
    });

    bard.verifyNoOutstandingHttpRequests();

});
