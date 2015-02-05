$(function() {
  function getIdFromUrl() {
    var url = window.location.pathname;
    function getId(url) {
      var split = url.split("/");
      return split[split.length-2];
    }
    return getId(url);
  }
  var id = getIdFromUrl();

  $.ajax({
    type: "GET",
    url: "/api/poll/" + id,
    success: function(data) {
      console.log(data);
      updatePieChart(data.tallies, data.options, data.total);
      watchForUpdates(data);
    },
    contentType: "application/json"
  })

  function watchForUpdates(poll) {
    function getPercentageTotal(index, total) {
      var percentage = Math.round(((poll.tallies[index] / total) || 0) * 100);
      return percentage;
    }

    function updateMeters() {
      var meters = $(".meter");
      meters.map(function(i, v) {
        var percentage = getPercentageTotal(i, poll.total);
        $(v).css("width", percentage + "%");
      })
    }
    updateMeters();

    var pollSocket = new WebSocket("ws://localhost:9000/ws/votes/" + id);
    pollSocket.onmessage = function (event) {
      var data    = JSON.parse(event.data);
      var tallies = data.tallies;
      var total   = data.total;
      poll.tallies = tallies;
      poll.total   = total;
      updatePieChart(tallies, poll.options, total);
      var meters = $(".meter");
      $("div div div.optionCount span.tally").map(function(i, v) {
        var percentage = getPercentageTotal(i, poll.total);
        $(meters[i]).css("width", percentage + "%");
        $(this).html("Votes: " + tallies[i] + " (" + percentage + "%)");
        return tallies[i];
      });
    }
  }



  function combineData(tallies, options) {
    var data = {};
    for (i = 0; i < tallies.length; i++) {
      data[options[i]] = tallies[i];
    }
    return data;
  }
  var cv_w = 300,
  cv_h = 300,
  cv_r = 150
  cv_color = d3.scale.category10();

  var cv_arc = d3.svg.arc().outerRadius(cv_r);
  var cv_pie = d3.layout.pie().sort(null).value(function (d) { return d.value });
  var cv_svg = d3.select("div.mydiv")
    .append("svg")
    .attr("width", cv_w)
    .attr("height", cv_h)
    .attr("style", "display:block; margin: 0 auto;")
    .append("g")
    .attr("transform", "translate(" + cv_r + "," + cv_r + ")");

  function cv_arcTween(a) {
    var i = d3.interpolate(this._current, a);
    this._current = i(0);
    return function(t) {
      return cv_arc(i(t));
    };
  }

  function tests (data) {
    console.log("updating");
    data = data ? data : {
      "slice1": Math.floor((Math.random()*10)+1),
      "slice2": Math.floor((Math.random()*10)+1),
      "slice3": Math.floor((Math.random()*10)+1),
      "slice4": Math.floor((Math.random()*10)+1) };
    var dataa = d3.entries(data);
    var cv_path = cv_svg.selectAll("path").data(cv_pie(dataa));
    var cv_text = cv_svg.selectAll("text").data(cv_pie(dataa));

    cv_path.enter()
      .append("path")
      .attr("fill", function(d, i) { return cv_color(i); } )
      .attr("d", cv_arc)
      .each(function(d) { this._current = d; });
    cv_text.enter()
      .append("text")
      .attr("transform", function(d) {
        d.innerRadius = 0;
        d.outerRadius = cv_r;
        return "translate(" + cv_arc.centroid(d) + ")";
      })
      .attr("text-anchor", "middle")
      .attr("font-weight", "bold")
      .attr("fill", "#FFFFFF")
      .attr("font-size", "20px")

    cv_text.text(function(d) {return d.data.key + "(" + d.data.value + ")";});

    cv_path.transition().duration(0).attrTween("d", cv_arcTween);
    cv_text.transition().duration(0).attr("transform", function(d) {
      d.innerRadius = 0;
      d.outerRadius = cv_r;
      return "translate(" + cv_arc.centroid(d) + ")";
    });

    cv_path.exit().remove();
    cv_text.exit().remove();
  }

  function updatePieChart(tallies, options, total) {
    var data = combineData(tallies, options);

    if (total < 1) {console.log("failed"); return;}
    tests(data);
  }

});
