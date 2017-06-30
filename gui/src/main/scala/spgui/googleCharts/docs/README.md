# Full Google Charts API
https://developers.google.com/chart/interactive/docs/reference#top_of_page

# Google API Example Use
See spgui/widgets/charts/TimelineWidget

# To use Google Charts Facades
To customaize Hover pop-up: See Tooltips
To set data: 1. create DataTable
             2. set columns (with or without help of DescriptionObject), read: addColumn()
             3. create row through Row-helper-class and add to table
To set options:
             1. See helper-classes for the chart
To draw:     GoogleVisualization.<Your chart>.draw(data: DataTable, options: js.Object)


# To implement new charts ( for more help - see googleCharts/timeline/ and GoogleVisualization.Timeline(..))
1. see the documentation for the chart
2. (googleCharts) create a new package (ex pieChart)
3. (googleCharts/<new package>/<chart options>) create a options helper-class
    that extends OptionsTrait (googleCharts/OptionsTrait.scala)
4. (googleCharts/<new package>/<chart row>) create a helper-class for each row
5. let the two helper classes have a method called something like
    Method          - toDynamic(): js.Object
    Arguments       - none
    Result value    - js.Object or one of its sub-classes
    Description     - takes all local variables and sets each value from the documentation
                      ( I used js.Dynamic.literal(title = this.title, ... ) )
6. Create a new javascript facade for the chart in GoogleVisualiztion.scala (see timeline)
7. use the helper-classes and GoogleVisualiztion in your Widget (see widgets/charts/TimelineWidget.scala)
   or implement a new Helper-class for easy usage