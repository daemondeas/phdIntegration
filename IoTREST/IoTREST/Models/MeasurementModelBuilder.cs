using Microsoft.OData.Edm;
using Microsoft.OData.Edm.Library;

namespace IoTREST.Models
{
    public class MeasurementModelBuilder
    {
        private readonly EdmModel _model = new EdmModel();
        private EdmEntityType _pulseOximetryType;
        private EdmEntityContainer _defaultContainer;
        private EdmEntitySet _pulseOximetrySet;

        public IEdmModel GetModel()
        {
            return _model;
        }

        public MeasurementModelBuilder BuildPulseOximetryType()
        {
            _pulseOximetryType = new EdmEntityType("IoTREST.Models", "PulseOximetryMeasurement");
            _pulseOximetryType.AddKeys(_pulseOximetryType.AddStructuralProperty("Id", EdmPrimitiveTypeKind.Int32));
            _pulseOximetryType.AddStructuralProperty("HeartRate", EdmPrimitiveTypeKind.Double);
            _pulseOximetryType.AddStructuralProperty("HeartRateUnit", EdmPrimitiveTypeKind.String);
            _pulseOximetryType.AddStructuralProperty("BloodOxygenSaturation", EdmPrimitiveTypeKind.Double);
            _pulseOximetryType.AddStructuralProperty("BloodOxygenSaturationUnit", EdmPrimitiveTypeKind.String);
            _pulseOximetryType.AddStructuralProperty("TimeStamp", EdmPrimitiveTypeKind.Date);
            _pulseOximetryType.AddStructuralProperty("PatientIdentification", EdmPrimitiveTypeKind.String);

            _model.AddElement(_pulseOximetryType);

            return this;
        }

        public MeasurementModelBuilder BuildDefaultContainer()
        {
            _defaultContainer = new EdmEntityContainer("IoTREST.Models", "DefaultContainer");
            _model.AddElement(_defaultContainer);

            return this;
        }

        public MeasurementModelBuilder BuildPulseOximetrySet()
        {
            _pulseOximetrySet = _defaultContainer.AddEntitySet("PulseOximetryMeasurements", _pulseOximetryType);

            return this;
        }
    }
}