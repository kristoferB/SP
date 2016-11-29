/* jshint -W117, -W030 */
describe('kubInputGUIControllerI4', function() {
    var controller;

    beforeEach(function() {
        bard.appModule('app.kubInputGUII4');
        bard.inject('$controller', '$log', '$rootScope', '$state');
        $state.current = {title: 'kubInputGUII4'};
    });

    beforeEach(function () {
        controller = $controller('kubInputGUIControllerI4');
        $rootScope.$apply();
    });

    bard.verifyNoOutstandingHttpRequests();

    describe('kubInputGUII4 controller', function() {
        it('should be created successfully', function () {
            expect(controller).to.be.defined;
        });

        describe('after activate', function() {
            it('should have empty log', function() {
                expect(controller.eventLog).to.equal([]);
            });

            it('should have logged "Activated"', function() {
                expect($log.info.logs).to.match(/Activated/);
            });
        });
    });
});
