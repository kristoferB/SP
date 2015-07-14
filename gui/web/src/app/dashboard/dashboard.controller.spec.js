/* jshint -W117, -W030 */
describe('DashboardController', function() {
    var controller;
    var models = mockData.getMockModels();

    beforeEach(function() {
        bard.appModule('app.dashboard');
        bard.inject('$controller', '$log', '$q', '$rootScope', 'spTalker');
    });

    beforeEach(function () {
        sinon.stub(spTalker, 'getModels').returns($q.when(models));
        controller = $controller('DashboardController');
        $rootScope.$apply();
    });

    bard.verifyNoOutstandingHttpRequests();

    describe('Dashboard controller', function() {
        it('should be created successfully', function () {
            expect(controller).to.be.defined;
        });

        describe('after activate', function() {
            it('should have title of Dashboard', function () {
                expect(controller.title).to.equal('Dashboard');
            });

            it('should have logged "Activated"', function() {
                expect($log.info.logs).to.match(/Activated/);
            });

            it('should have news', function () {
                expect(controller.news).to.not.be.empty;
            });

            it('should have at least 1 model', function () {
                expect(controller.models).to.have.length.above(0);
            });

            it('should have model count of 2', function () {
                expect(controller.models).to.have.length(2);
            });
        });
    });
});
