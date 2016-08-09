/**
 * Created by edvard on 2016-04-06.
 */

import {Component, OnInit} from '@angular/core';
import * as d3 from 'd3';
import {SocketIO} from './socket-io';

@Component({
    selector: 'faces',
    template: `
        <div style="position:relative;">
        <svg class="face_chart" style="display: block; margin:0 auto; font-weight:bold;" ></svg>
            <img id="bertilpil" class="fejsss" src="app/erica-components/icons/Trend_ok.svg"
                width="45%"
                height="45%" />
            <img id="lottapil" class="fejsss" src="app/erica-components/icons/Trend_bra.svg"
                width="45%"
                height="45%" />
        </div>
        `,
    styles: [`
        .fejsss {
            position:absolute;
        }
        #bertil {
            top: 37%;
            left: -10.5%;
        }
        #lotta {
            top: 37%;
            left: 65%;
        }
        #bertilpil {
            top: 70%;
            left: 2.5%;
        } 
        #lottapil {
            top: 70%;
            left: 53%;
        } 
        `],
    providers: [SocketIO]
})

export class Faces implements OnInit {

    private static scaleSVG(svg,width,height,endpoints){
        svg.attr("viewBox", endpoints[0] +" " +endpoints[1] +" " +endpoints[2] +" " +endpoints[3]);
        svg.attr("preserveAspectRatio","xMaxYMax");
        svg.attr("height",height +"%");
        svg.attr("width",width +"%");
    }


    static svgClass = '.face_chart';
    static chart;

    ngOnInit() {
        var data = {
            'ttd': {
                'value': 129,
                'trend': 1, // -1, 0 o 1..?
                'mood': 0, // -1, 0 o 1..?
            },
            'ttk': {
                'value': 329,
                'trend': 0,
                'mood': 1
            }
        };

        Faces.draw(data);

        SocketIO.subscribe('smile_face_blue', function(data) {
            Faces.draw(data);
        });
    }

    static draw(data) {
        this.chart = d3.select(this.svgClass);
        this.scaleSVG(this.chart,90,100,[0,0,250,105]);
        this.chart.selectAll("*").remove();

        var distance = 105, radius = 50, circle1X = radius + 20, circleY = radius + 2;
        var face1X = 30, faceY = 25, faceR = radius * 0.4;
        var arrow1X = 15, arrowY = 50;

        this.drawCircle(circle1X, circleY, radius, data['ttd'],'TTL');
        var picPath1 = this.moodPic(data['ttd']['mood']);
        d3.select("#bertil").attr("src", picPath1);
        var arrowPicPath1 = this.arrowPic(data['ttd']['trend']);
        d3.select("#bertilpil").attr("src", arrowPicPath1);

        this.drawCircle(circle1X + distance, circleY, radius, data['ttk'],'TTK');
        var picPath2 = this.moodPic(data['ttk']['mood']);
        d3.select("#lotta").attr("src", picPath2);
        var arrowPicPath2 = this.arrowPic(data['ttd']['trend']);
        d3.select("#lottapil").attr("src", arrowPicPath2);
    }

    static moodPic(mood: number) {
        var picPath: string;
        switch(mood) {
            case 1:
                picPath = "app/erica-components/icons/Nyckeltal_bra.svg";
                break;
            case 0:
                picPath = "app/erica-components/icons/Nyckeltal_ok.svg";
                break;
            case -1:
                picPath = "app/erica-components/icons/Nyckeltal_bad.svg";
                break;
        }
        return picPath;
    }

    static arrowPic(trend: number) {
        var picPath: string;
        switch(trend) {
            case 1:
                picPath = "app/erica-components/icons/Trend_bra.svg";
                break;
            case 0:
                picPath = "app/erica-components/icons/Trend_ok.svg";
                break;
            case -1:
                picPath = "app/erica-components/icons/Trend_bad.svg";
                break;
        }
        return picPath;
    }
    static drawCircle(circleX, circleY, circleR, datattx, typeText:string) { // datattx är data['ttd'] eller data['ttk']
        var chart = this.chart;

        chart.append("circle")
            .attr("cx", circleX)
            .attr("cy", circleY)
            .attr("r", circleR)
            .attr("angle", 360)
            .style("fill", "#bfbfbf")
            .style("stroke-width",2)
            .style("stroke","rgb(95, 95, 95)");

        chart.append("text")
            .attr('x', circleX - circleR * 0.465)
            .attr('y', circleY - circleR * 0.2)
            .text(datattx['value'] + "min"); //was: text(Math.round(datattx['value']));

        chart.append("text")
            .attr('x', circleX - circleR * 0.23)
            .attr('y', circleY + circleR * 0.2)
            .text(typeText);
    }

    static drawFace(faceX, faceY, faceR, datattx) { // datattx är data['ttd'] eller data['ttk']

        var chart = this.chart;

        switch(datattx['mood']) {
            case 1:
                chart.append("circle")
                    .attr("cx", faceX)
                    .attr("cy", faceY)
                    .attr("r", faceR)
                    .attr("angle", 360)
                    .style("stroke-width",1)
                    .style("stroke","rgb(95, 95, 95)")
                    .style("fill", "#c7e0ae");;

                var arc = d3.svg.arc()
                    .innerRadius(faceR * 0.5)
                    .outerRadius(faceR * 0.6)
                    .startAngle(120 * (Math.PI/180)) //converting from degs to radians
                    .endAngle(240 * (Math.PI/180)); //just radians

                chart.append("path")
                    .attr("d", arc)
                    .attr("transform", "translate(" + faceX + "," + faceY + ")");
                break;
            case 0:
                chart.append("circle")
                    .attr("cx", faceX)
                    .attr("cy", faceY)
                    .attr("r", faceR)
                    .attr("angle", 360)
                    .style("stroke-width",1)
                    .style("stroke","rgb(95, 95, 95)")
                    .style("fill", "#fcf0b4");;

                var arc = d3.svg.arc()
                    .innerRadius(faceR * 1)
                    .outerRadius(faceR * 1.1)
                    .startAngle(168 * (Math.PI/180)) //converting from degs to radians
                    .endAngle(192 * (Math.PI/180)); //just radians

                chart.append("path")
                    .attr("d", arc)
                    .attr("transform", "translate(" + faceX + "," + (faceY - faceR * 0.5) + ")");
                break;
            case -1:
                chart.append("circle")
                    .attr("cx", faceX)
                    .attr("cy", faceY)
                    .attr("r", faceR)
                    .attr("angle", 360)
                    .style("stroke-width",1)
                    .style("stroke","rgb(95, 95, 95)")
                    .style("fill", "#f9b9b9");;

                var arc = d3.svg.arc()
                    .innerRadius(faceR * 0.5)
                    .outerRadius(faceR * 0.6)
                    .startAngle(-50 * (Math.PI/180)) //converting from degs to radians
                    .endAngle(50 * (Math.PI/180)); //just radians

                chart.append("path")
                    .attr("d", arc)
                    .attr("transform", "translate(" + faceX + "," + (faceY + faceR * 0.8) + ")");
                break;
        }

        chart.append("circle")
            .attr("cx", faceX + faceR * 0.22)
            .attr("cy", faceY - faceR * 0.2)
            .attr("r", faceR * 0.1)
            .attr("angle", 360)
            .style("fill", "black");

        chart.append("circle")
            .attr("cx", faceX - faceR * 0.22)
            .attr("cy", faceY - faceR * 0.2)
            .attr("r", faceR * 0.1)
            .attr("angle", 360)
            .style("fill", "black");
    }

    static drawArrow(arrowX, arrowY, datattx) { // datattx är data['ttd'] eller data['ttk']
        var chart = this.chart;

        var length = 40, width = 10, headWidth = 20;

        var poly = [{"x": arrowX + (headWidth - width) / 2.0, "y": arrowY + 0},
            {"x": arrowX + (headWidth - width) / 2.0, "y": arrowY + length - headWidth * 0.64},
            {"x": arrowX + 0, "y": arrowY + length - headWidth * 0.64},
            {"x": arrowX + headWidth / 2.0, "y": arrowY + length},
            {"x": arrowX + headWidth, "y": arrowY + length - headWidth * 0.64},
            {"x": arrowX + headWidth - (headWidth - width) / 2.0, "y": arrowY + length - headWidth * 0.64},
            {"x": arrowX + headWidth - (headWidth - width) / 2.0, "y": arrowY + 0}
        ];

        var points = "";
        for(var i = 0; i < poly.length; i++) { // genererar en sträng i format som CSS vill ha...
            points = points.concat(poly[i].x + "," + poly[i].y + " ");
        }

        var arrow = chart
            .append("polygon")
            .attr("points", points)
            .style("stroke-width",1)
            .style("stroke","rgb(95, 95, 95)");


        var midX = arrowX + headWidth / 2.0, midY = arrowY + length / 2.0;
        var rotation, color;
        switch(datattx['trend']) {
            case -1:
                rotation = 0;
                color = "#c7e0ae";
                break;
            case 0:
                rotation = 270;
                color = "#a9daea";
                break;
            case 1:
                rotation = 180;
                color = "#f9b9b9";
                break;
        }
        arrow.attr("transform", "rotate(" + rotation + "," + midX + "," + midY + ")");
        arrow.attr("fill", color);
    }
}
