/* jshint -W117, -W030 */
describe('Tobbe2Controller', function() {
    var controller;

    beforeEach(function() {
        bard.appModule('app.Tobbe2');
        bard.inject('$controller', '$log', '$rootScope', '$state');
        $state.current = {title: 'Tobbe2'};
    });

    beforeEach(function () {
        controller = $controller('Tobbe2Controller');
        $rootScope.$apply();
    });

    bard.verifyNoOutstandingHttpRequests();

    describe('Tobbe2 controller', function() {
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
