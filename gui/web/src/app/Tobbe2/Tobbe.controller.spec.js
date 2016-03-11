/* jshint -W117, -W030 */
describe('TobbeController', function() {
    var controller;

    beforeEach(function() {
        bard.appModule('app.Tobbe');
        bard.inject('$controller', '$log', '$rootScope', '$state');
        $state.current = {title: 'Tobbe'};
    });

    beforeEach(function () {
        controller = $controller('TobbeController');
        $rootScope.$apply();
    });

    bard.verifyNoOutstandingHttpRequests();

    describe('Tobbe controller', function() {
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
