# cma-editor
**Read** and **write** contents of **camera files** for both **PS2** &amp; **Wii** versions of the **Budokai Tenkaichi** games.
Requires Java SE 8 or higher.

# Preview/Usage
Top CMA is **overwritten**, bottom CMA is the **original**.
``-wm 1 4`` in this case **multiplies** the values of the **POS-Y section** by **4**.
![image](https://github.com/user-attachments/assets/d755522e-6b6c-49c7-a359-6bed1d0a3509)

Here is what the program displays when the ``-h`` argument is used:
```
Read and write CMA contents for both PS2 & Wii versions of Budokai Tenkaichi games.
Here is a list of all the arguments that can be used. Use -h or -help to print this out again.

* -r --> Read CMA files and write their contents in *.txt files with the same file names.
* -wm -> Overwrite CMA files by multiplying their contents by a coefficient.
* -wa -> Overwrite CMA files by adding a coefficient to their contents.

For the write commands, the first argument must be the number of the section that will be edited:
0 = POS-X, 1 = POS-Y, 2 = POS-Z, 3 = ROT-Y, 4 = ROT-X, 5 = ROT-Z.
NOTE: For this program, X is left/right, Y is up/down, Z is back/forward.

As for the second argument, it must be a coefficient (whole number or decimal).
```
