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
      updatePoll(data);
      watchForUpdates(data);
    },
    contentType: "application/json"
  })

  function getPercentageTotal(tally, total) {
    var percentage = Math.round(((tally / total) || 0) * 100);
    return percentage;
  }

  function watchForUpdates(poll) {
    var pollSocket = new WebSocket("ws://" + window.location.hostname + "/ws/votes/" + id);
    pollSocket.onmessage = function (event) {
      var data    = JSON.parse(event.data);
      poll.tallies = data.tallies;
      poll.total   = data.total;
      updatePoll(poll)
    }
  }

  function formatForPlural(word, count) {
    if (count === 0) return word + "s"
    if (count > 1) return word + "s"
    return word
  }

  function formatTotalCount(total) {
    if (total === 0) return "0 votes";
    if (total === 1) return "1 vote";
    return total + " total votes";
  }

  function updatePoll(poll) {
    updatePieChart(poll.tallies, poll.options, poll.total);
    var meters = $(".meter");
    var sortedUnzipped = _.zip.apply(_, _.sortBy(_.zip(poll.options, poll.tallies), function(z) {
      return z[1];
    }).reverse());
    var options = sortedUnzipped[0];
    var tallies = sortedUnzipped[1];
    $(".totalVotes").html(formatTotalCount(poll.total));
    $("div div div.optionCount span.tally").map(function(i, v) {
      $($(".pollBody span.optionNumber")[i]).html((i+1) + ".");
      $($(".pollBody span.optionResult")[i]).html(options[i]);
      var percentage = getPercentageTotal(tallies[i], poll.total);
      $(meters[i]).css("width", percentage + "%");
      $(this).html(tallies[i] + " " + formatForPlural("vote", tallies[i]) + " (" + percentage + "%)");
      return tallies[i];
    });
  }

  function startHighlightTextEvent() {
    var paths = $(".pie svg>g path");
    $(paths).prependTo(".pie svg>g");
    var texts = $(".pie svg>g text");
    _.each(paths, function(p, i) {
      $(p).unbind("hover");
      $(p).hover(function() {
        $(p).attr("class", "hover");
        $(texts[i]).attr("class", "hover");
      }, function() {
        $(p).attr("class", "");
        $(texts[i]).attr("class", "");
      });
    })
  }

  function combineData(tallies, options, total) {
    var data = [];
    for (i = 0; i < tallies.length; i++) {
      var currentOption = options[i];
      var currentTally = tallies[i];
      dataObj = {
        option: currentOption,
        tally: currentTally,
        total: total
      };
      data.push(dataObj);
    }
    return data;
  }

  $(".content").after("<div class='pie'></div>");

  cv_w = 300,
  cv_h = 300,
  cv_r = 150 ,
  cv_color = d3.scale.category20();

  var arc = d3.svg.arc().outerRadius(cv_r);
  var cv_arc = d3.svg.arc().outerRadius(cv_r);
  //.sort(null) ??
  var cv_pie = d3.layout.pie().value(function (d) { return d.value.tally });
  var cv_svg = d3.select("div.pie")
    .append("svg")
    .attr("width", cv_w)
    .attr("height", cv_h)
    .append("g")
    .attr("transform",
          "translate(" + cv_r + "," + cv_r + ")");

  function cv_arcTween(a) {
    var i = d3.interpolate(this._current, a);
    this._current = i(0);
    return function(t) {
      return cv_arc(i(t));
    };
  }

  function tests (data) {
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
      .each(function(d) {
        this._current = d;
      })
      .attr("stroke","white")
      .attr("stroke-width",1)

    cv_text.enter()
      .append("text")
      .attr("transform", function(d) {
        d.innerRadius = 0;
        d.outerRadius = cv_r;
        return "translate(" + cv_arc.centroid(d) + ")";
      })

    cv_text.text(function(d) {
      var tally = d.data.value.tally;
      var total = d.data.value.total;
      return d.data.value.option + "(" + getPercentageTotal(tally, total) + "%)";});

    cv_text.filter(function(d) {
      var tally = d.data.value.tally;
      var total = d.data.value.total
      return getPercentageTotal(tally, total) <= 5;
    }).attr("visibility", "hidden");

    cv_text.filter(function(d) {
      var tally = d.data.value.tally;
      var total = d.data.value.total
      return getPercentageTotal(tally, total) >= 5;
    }).attr("visibility", "visible");



    cv_path.transition().duration(0).attrTween("d", cv_arcTween);
    cv_text.transition().duration(0).attr("transform", function(d) {
      d.innerRadius = 0;
      d.outerRadius = cv_r;
      return "translate(" + cv_arc.centroid(d) + ")";
    });

    cv_path.exit().remove();
    cv_text.exit().remove();
    startHighlightTextEvent();
  }

  function updatePieChart(tallies, options, total) {
    var data = combineData(tallies, options, total);

    filteredData = _.filter(data, function(obj) {
      return obj.tally > 0;
    })

    if (total < 1) {console.log("failed"); return;}
    tests(filteredData);
  }

});
