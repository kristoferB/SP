/**
 * Created by daniel on 2015-08-05.
 */
/* jshint -W117, -W030 */
describe('spWidget', function() {
    var compiledElement;

    beforeEach(function() {
        bard.appModule('app.widgets');
        bard.inject('$compile', '$rootScope', '$httpBackend');

        $rootScope.title = 'My Widget';

        // Compile a piece of HTML containing the directive
        var templateElement = angular.element(
            '<sp-widget widget-title="title">'                  +
                '<div class="stuff">Some content</div>'         +
                '<table>'                                       +
                    '<thead>'                                   +
                        '<tr>'                                  +
                            '<th>Name</th>'                     +
                            '<th>Kind</th>'                     +
                            '<th>Date</th>'                     +
                        '</tr>'                                 +
                    '</thead>'                                  +
                    '<tbody>'                                   +
                        '<tr>'                                  +
                            '<td>Robot 2</td>'                  +
                            '<td>Thing</td>'                    +
                            '<td>2015-08-06</td>'               +
                        '</tr>'                                 +
                    '</tbody>'                                  +
                    '<tfoot>'                                   +
                        '<tr>'                                  +
                            '<td colspan="3">Footer stuff</td>' +
                        '</tr>'                                 +
                    '</tfoot>'                                  +
                '</table>'                                      +
            '</sp-widget>');

        $httpBackend.whenGET(/sp-widget.html/).respond(function() {
            var request = new XMLHttpRequest();
            request.open('GET', 'app/widgets/sp-widget.html', false);
            request.send(null);
            return [request.status, request.response, {}];
        });

        compiledElement = $compile(templateElement)($rootScope);

        // Fire all the watches, so the scope expressions are evaluated
        $rootScope.$digest();
        $httpBackend.flush();
    });

    it('should exist', function() {
        expect(compiledElement.find('.panel').length).to.be.defined;
    });

    it('should have the title "My Widget"', function() {
        expect(compiledElement.find('.panel-title').text()).to.equal('My Widget');
    });

    it('should have transcluded in the div tag', function() {
        expect(compiledElement.find('.stuff').text()).to.equal('Some content');
    });

    it('should have transcluded in the table header', function() {
        expect(compiledElement.find('th').first().text()).to.equal('Name');
        expect(compiledElement.find('th').last().text()).to.equal('Date');
    });

    it('should have transcluded in the table body', function() {
        expect(compiledElement.find('tbody').find('td').first().text()).to.equal('Robot 2');
        expect(compiledElement.find('tbody').find('td').last().text()).to.equal('2015-08-06');
    });

    it('should have transcluded in the table footer', function() {
        expect(compiledElement.find('tfoot').find('td').text()).to.equal('Footer stuff');
    });

});
