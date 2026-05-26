import tensorflow as tf
import os
import sys

model_path = os.path.join(os.path.dirname(__file__), 'efficientnetv2s.tflite')
print('MODEL_PATH:', model_path)
print('FILE_EXISTS:', os.path.exists(model_path))
if not os.path.exists(model_path):
    print('ERROR: model file not found')
    sys.exit(2)
print('FILE_SIZE_BYTES:', os.path.getsize(model_path))

try:
    interpreter = tf.lite.Interpreter(model_path=model_path)
    interpreter.allocate_tensors()
    inp = interpreter.get_input_details()
    out = interpreter.get_output_details()
    print('NUM_INPUTS:', len(inp))
    for i,d in enumerate(inp):
        print(f"INPUT[{i}] name={d.get('name')} shape={d.get('shape').tolist() if hasattr(d.get('shape'), 'tolist') else d.get('shape')} dtype={d.get('dtype')}")
    print('NUM_OUTPUTS:', len(out))
    for i,d in enumerate(out):
        print(f"OUTPUT[{i}] name={d.get('name')} shape={d.get('shape').tolist() if hasattr(d.get('shape'), 'tolist') else d.get('shape')} dtype={d.get('dtype')}")
    try:
        print('SIGNATURES:', interpreter.get_signature_list())
    except Exception as e:
        print('SIGNATURES_ERROR:', e)
    try:
        td = interpreter.get_tensor_details()
        print('TENSOR_COUNT:', len(td))
    except Exception as e:
        print('TENSOR_DETAILS_ERROR:', e)
    # infer classes if possible
    if len(out) >= 1:
        out_shape = out[0].get('shape')
        try:
            print('INFERRED_NUM_CLASSES:', int(out_shape[-1]))
        except Exception:
            print('INFERRED_NUM_CLASSES: unknown')
    print('OK: model loaded')
except Exception as e:
    print('ERROR_LOADING:', e)
    sys.exit(3)
