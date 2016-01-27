using System;
using System.Collections.Generic;
using System.Linq;
using System.Net;
using System.Net.Http;
using System.Web.Http;
using IoTREST.Models;
using IoTREST.Services;

namespace IoTREST.Controllers
{
    public class PulseOximetryController : ApiController
    {
        private PulseOximetryMeasurementRepository pulseOximetryMeasurementRepository;

        public PulseOximetryController()
        {
            pulseOximetryMeasurementRepository = new PulseOximetryMeasurementRepository();
        }

        public PulseOximetryMeasurement[] Get()
        {
            return pulseOximetryMeasurementRepository.GetAllMeasurements();
        }

        public HttpResponseMessage Post(PulseOximetryMeasurement measurement)
        {
            pulseOximetryMeasurementRepository.SavePulseOximetryMeasurement(measurement);

            var response = Request.CreateResponse<PulseOximetryMeasurement>(System.Net.HttpStatusCode.Created,
                measurement);

            return response;
        }
    }
}
