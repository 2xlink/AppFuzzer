"""
===============
Basic pie chart
===============

Demo of a basic pie chart plus a few additional features.

In addition to the basic pie chart, this demo shows a few optional features:

    * slice labels
    * auto-labeling the percentage
    * offsetting a slice with "explode"
    * drop-shadow
    * custom start angle

Note about the custom start angle:

The default ``startangle`` is 0, which would start the "Frogs" slice on the
positive x-axis. This example sets ``startangle = 90`` such that everything is
rotated counter-clockwise by 90 degrees, and the frog slice starts on the
positive y-axis.
"""
import matplotlib.pyplot as plt

labels = 'Successful without failures', 'Successful, but app crashes', 'Failed to install (unknown error)', 'Failed to install (unsupported architecture)', 'Failed at runtime', 'Failed due to timeout'
with open("logs/packagelist_successful", "r") as file:
    # Get line count
    successful = sum(1 for line in file) - 1
with open("logs/packagelist_successful_crash", "r") as file:
    successful_crash = sum(1 for line in file) - 1
with open("logs/packagelist_failed_to_install_unknown_error", "r") as file:
    failed_to_install_unknown_error = sum(1 for line in file) - 1
with open("logs/packagelist_failed_to_install_unsupported_arch", "r") as file:
    failed_to_install_unsupported_arch = sum(1 for line in file) - 1
with open("logs/packagelist_failed_to_start", "r") as file:
    failed_to_start = sum(1 for line in file) - 1
with open("logs/packagelist_failed_timeout", "r") as file:
    failed_timeout = sum(1 for line in file) - 1
total = successful + successful_crash + failed_timeout + failed_to_install_unknown_error + failed_to_install_unsupported_arch + failed_to_start

# Pie chart, where the slices will be ordered and plotted counter-clockwise:
sizes = [successful / total, successful_crash / total, failed_to_install_unknown_error / total, failed_to_install_unsupported_arch / total, failed_to_start / total, failed_timeout / total]
# explode = (0, 0, 0, 0)  # only "explode" the 2nd slice (i.e. 'Hogs')

fig1, ax1 = plt.subplots()
ax1.pie(sizes, labels=labels, autopct='%1.1f%%',
        shadow=False, startangle=90)
ax1.axis('equal')  # Equal aspect ratio ensures that pie is drawn as a circle.

plt.show()