/* jshint -W117, -W030 */
describe('kubInputGUIController', function() {
    var controller;

    beforeEach(function() {
        bard.appModule('app.kubInputGUI');
        bard.inject('$controller', '$log', '$rootScope', '$state');
        $state.current = {title: 'kubInputGUI'};
    });

    beforeEach(function () {
        controller = $controller('kubInputGUIController');
        $rootScope.$apply();
    });

    bard.verifyNoOutstandingHttpRequests();

    describe('kubInputGUI controller', function() {
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
